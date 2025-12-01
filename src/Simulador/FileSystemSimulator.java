package Simulador;

import java.io.*;
import java.util.*;

public class FileSystemSimulator implements Serializable {
    private static final long serialVersionUID = 1L;
    private DirectoryNode root;
    private transient Journal journal;
    private String imagePath;
    private String journalPath;

    public FileSystemSimulator(String imagePath, String journalPath) {
        this.imagePath = imagePath;
        this.journalPath = journalPath;
        this.journal = new Journal(journalPath);
        if (!loadImage()) {
            root = new DirectoryNode("/");
            persistImage();
        } else {
            this.journal = new Journal(journalPath);
            try { recoverFromJournal(); } catch (Exception ex) { System.err.println("Recovery failed: " + ex.getMessage()); }
        }
    }

    // Carrega o sistema de arquivos salvo em disco (fs.img)
    private boolean loadImage() {
        File f = new File(imagePath);
        if (!f.exists()) return false;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            FileSystemSimulator loaded = (FileSystemSimulator) ois.readObject();
            this.root = loaded.root;
            return true;
        } catch (Exception e) {
            System.err.println("Erro carregando imagem: " + e.getMessage());
            return false;
        }
    }

    // Salva todo o sistema de arquivos no arquivo de imagem
    private synchronized void persistImage() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(imagePath))) {
            oos.writeObject(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Garante que o journal está inicializado
    private void ensureJournal() { if (journal == null) journal = new Journal(journalPath); }


    // Divide um caminho em partes individuais
    private String[] splitPath(String path) {
        String p = path.trim();
        if (p.equals("/")) return new String[0];
        if (p.startsWith("/")) p = p.substring(1);
        return p.split("/");
    }


    // Navega até o diretório pai do caminho informado
    private DirectoryNode traverseParent(String path) {
        String[] parts = splitPath(path);
        DirectoryNode cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            FsNode n = cur.get(parts[i]);
            if (n == null || !n.isDirectory()) return null;
            cur = (DirectoryNode) n;
        }
        return cur;
    }


    // Navega até o diretório pai do caminho informado
    private FsNode traverse(String path) {
        if (path.equals("/")) return root;
        String[] parts = splitPath(path);
        DirectoryNode cur = root;
        for (int i = 0; i < parts.length; i++) {
            FsNode n = cur.get(parts[i]);
            if (n == null) return null;
            if (i == parts.length - 1) return n;
            if (!n.isDirectory()) return null;
            cur = (DirectoryNode) n;
        }
        return null;
    }


    // Cria um novo diretório e registra a operação no journal
    public synchronized void mkdir(String path) throws IOException {
        ensureJournal();
        Map<String,String> params = new HashMap<>();
        params.put("path", path);
        journal.appendPending("MKDIR", params);

        DirectoryNode parent = traverseParent(path);
        if (parent == null) throw new IOException("Caminho inválido");
        String[] parts = splitPath(path);
        String name = parts[parts.length - 1];
        if (parent.get(name) != null) throw new IOException("Já existe");
        parent.add(name, new DirectoryNode(name));
        persistImage();
        List<Journal.Entry> pend = journal.pending();
        if (!pend.isEmpty()) journal.markCommit(pend.get(pend.size()-1).id);
    }

    // Remove um diretório vazio e registra no journal
    public synchronized void rmdir(String path) throws IOException {
        ensureJournal();
        Map<String,String> params = new HashMap<>();
        params.put("path", path);
        journal.appendPending("RMDIR", params);

        FsNode node = traverse(path);
        if (node == null || !node.isDirectory()) throw new IOException("Não existe ou não é diretório");
        DirectoryNode dir = (DirectoryNode) node;
        if (!dir.isEmpty()) throw new IOException("Diretório não está vazio");
        DirectoryNode parent = traverseParent(path);
        String[] parts = splitPath(path);
        parent.remove(parts[parts.length -1]);
        persistImage();
        List<Journal.Entry> pend = journal.pending();
        if (!pend.isEmpty()) journal.markCommit(pend.get(pend.size()-1).id);
    }


    // Cria um arquivo vazio e registra no journal
    public synchronized void touch(String path) throws IOException {
        ensureJournal();
        Map<String,String> params = new HashMap<>();
        params.put("path", path);
        journal.appendPending("TOUCH", params);

        DirectoryNode parent = traverseParent(path);
        if (parent == null) throw new IOException("Caminho inválido");
        String[] parts = splitPath(path);
        String name = parts[parts.length - 1];
        if (parent.get(name) == null) parent.add(name, new FileNode(name));
        persistImage();
        List<Journal.Entry> pend = journal.pending();
        if (!pend.isEmpty()) journal.markCommit(pend.get(pend.size()-1).id);
    }


    // Escreve conteúdo em um arquivo e registra no journal
    public synchronized void writeFile(String path, String content) throws IOException {
        ensureJournal();
        Map<String,String> params = new HashMap<>();
        params.put("path", path); params.put("content", content);
        journal.appendPending("WRITE", params);

        FsNode n = traverse(path);
        if (n == null) { touch(path); n = traverse(path); }
        if (n.isDirectory()) throw new IOException("É diretório");
        FileNode f = (FileNode) n;
        f.write(content);
        persistImage();
        List<Journal.Entry> pend = journal.pending();
        if (!pend.isEmpty()) journal.markCommit(pend.get(pend.size()-1).id);
    }


    // Lê o conteúdo de um arquivo
    public synchronized String readFile(String path) throws IOException {
        FsNode n = traverse(path);
        if (n == null || n.isDirectory()) throw new IOException("Arquivo não existe");
        return ((FileNode) n).read();
    }


    // Remove um arquivo ou diretório e registra no journal
    public synchronized void rm(String path) throws IOException {
        ensureJournal();
        Map<String,String> params = new HashMap<>();
        params.put("path", path);
        journal.appendPending("RM", params);

        DirectoryNode parent = traverseParent(path);
        if (parent == null) throw new IOException("Caminho inválido");
        String[] parts = splitPath(path);
        FsNode rem = parent.remove(parts[parts.length - 1]);
        if (rem == null) throw new IOException("Não encontrado");
        persistImage();
        List<Journal.Entry> pend = journal.pending();
        if (!pend.isEmpty()) journal.markCommit(pend.get(pend.size()-1).id);
    }


    // Lista os arquivos e diretórios dentro de um caminho
    public synchronized List<String> ls(String path) throws IOException {
        FsNode n = traverse(path);
        if (n == null) throw new IOException("Caminho inválido");
        if (!n.isDirectory()) throw new IOException("Não é diretório");
        DirectoryNode d = (DirectoryNode)n;
        return new ArrayList<>(d.getChildren().keySet());
    }


    // Move ou renomeia arquivo/diretório e registra no journal
    public synchronized void mv(String src, String dst) throws IOException {
        ensureJournal();
        Map<String,String> params = new HashMap<>();
        params.put("src", src); params.put("dst", dst);
        journal.appendPending("MV", params);

        DirectoryNode psrc = traverseParent(src);
        DirectoryNode pdst = traverseParent(dst);
        if (psrc == null || pdst == null) throw new IOException("Caminho inválido");
        String[] sps = splitPath(src);
        String sname = sps[sps.length-1];
        FsNode node = psrc.remove(sname);
        if (node == null) throw new IOException("Origem não existe");
        String[] dps = splitPath(dst);
        String dname = dps[dps.length-1];
        node.setName(dname);
        pdst.add(dname, node);
        persistImage();
        List<Journal.Entry> pend = journal.pending();
        if (!pend.isEmpty()) journal.markCommit(pend.get(pend.size()-1).id);
    }


    // Copia um arquivo ou diretório e registra no journal
    public synchronized void cp(String src, String dst) throws IOException {
        ensureJournal();
        Map<String,String> params = new HashMap<>();
        params.put("src", src); params.put("dst", dst);
        journal.appendPending("CP", params);

        FsNode n = traverse(src);
        if (n == null) throw new IOException("Origem não existe");
        DirectoryNode pdst = traverseParent(dst);
        if (pdst == null) throw new IOException("Destino inválido");
        String[] dps = splitPath(dst);
        String dname = dps[dps.length-1];

        if (n.isDirectory()) {
            DirectoryNode dcopy = new DirectoryNode(dname);
            DirectoryNode orig = (DirectoryNode) n;
            for (Map.Entry<String, FsNode> e : orig.getChildren().entrySet()) {
                FsNode child = e.getValue();
                if (child.isDirectory()) {
                    dcopy.add(child.getName(), new DirectoryNode(child.getName()));
                } else {
                    FileNode fn = new FileNode(child.getName());
                    fn.write(((FileNode)child).read());
                    dcopy.add(fn.getName(), fn);
                }
            }
            pdst.add(dname, dcopy);
        } else {
            FileNode fn = (FileNode) n;
            FileNode copy = new FileNode(dname);
            copy.write(fn.read());
            pdst.add(dname, copy);
        }
        persistImage();
        List<Journal.Entry> pend = journal.pending();
        if (!pend.isEmpty()) journal.markCommit(pend.get(pend.size()-1).id);
    }


    // Reexecuta operações pendentes do journal para recuperar estado consistente
    private void recoverFromJournal() throws IOException {
        ensureJournal();
        List<Journal.Entry> pending = journal.pending();
        for (Journal.Entry e : pending) {
            try {
                switch (e.type) {
                    case "MKDIR": mkdir(e.params.get("path"));
                    break;
                    case "RMDIR": rmdir(e.params.get("path"));
                    break;
                    case "TOUCH": touch(e.params.get("path"));
                    break;
                    case "WRITE": writeFile(e.params.get("path"), e.params.get("content"));
                    break;
                    case "RM": rm(e.params.get("path"));
                    break;
                    case "MV": mv(e.params.get("src"), e.params.get("dst"));
                    break;
                    case "CP": cp(e.params.get("src"), e.params.get("dst"));
                    break;
                }
                journal.markCommit(e.id);
            } catch (Exception ex) {
                System.err.println("Erro ao reaplicar entrada do journal: " + ex.getMessage());
            }
        }
    }
}
