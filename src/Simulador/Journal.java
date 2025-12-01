package Simulador;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * Journal simples sem dependências externas.
 * Formato por linha:
 * id \t type \t status \t ts \t paramsEncoded
 *
 * paramsEncoded = chave1=valor1&chave2=valor2 (valores URL-encoded)
 */
public class Journal {
    private final File file;

    public static class Entry {
        public String id;
        public String type;
        public Map<String,String> params;
        public String status;
        public long ts;
    }

    public Journal(String path) {
        this.file = new File(path);
    }

    // append entrada PENDING
    public synchronized void appendPending(String type, Map<String,String> params) throws IOException {
        Entry e = new Entry();
        e.id = UUID.randomUUID().toString();
        e.type = type;
        e.params = params == null ? new HashMap<>() : new HashMap<>(params);
        e.status = "PENDING";
        e.ts = System.currentTimeMillis();
        try (FileWriter fw = new FileWriter(file, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(serialize(e));
            bw.newLine();
        }
    }

    // marcar commit (altera status para COMMIT reescrevendo o arquivo)
    public synchronized void markCommit(String id) throws IOException {
        List<Entry> all = readAll();
        boolean changed = false;
        for (Entry e : all) {
            if (e.id.equals(id) && !"COMMIT".equals(e.status)) {
                e.status = "COMMIT";
                changed = true;
            }
        }
        if (changed) {
            try (FileWriter fw = new FileWriter(file, false);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                for (Entry e : all) {
                    bw.write(serialize(e));
                    bw.newLine();
                }
            }
        }
    }

    // lê todas as entradas
    public synchronized List<Entry> readAll() throws IOException {
        List<Entry> list = new ArrayList<>();
        if (!file.exists()) return list;
        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                Entry e = deserialize(line);
                if (e != null) list.add(e);
            }
        }
        return list;
    }

    // retorna somente as pendentes
    public synchronized List<Entry> pending() throws IOException {
        List<Entry> out = new ArrayList<>();
        for (Entry e : readAll()) if ("PENDING".equals(e.status)) out.add(e);
        return out;
    }

    /* ---------- helpers de serialização simples ---------- */

    private String serialize(Entry e) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append(escape(e.id)).append("\t");
        sb.append(escape(e.type)).append("\t");
        sb.append(escape(e.status)).append("\t");
        sb.append(e.ts).append("\t");
        sb.append(encodeParams(e.params));
        return sb.toString();
    }

    private Entry deserialize(String line) {
        try {
            String[] parts = line.split("\t", 5);
            if (parts.length < 5) return null;
            Entry e = new Entry();
            e.id = unescape(parts[0]);
            e.type = unescape(parts[1]);
            e.status = unescape(parts[2]);
            e.ts = Long.parseLong(parts[3]);
            e.params = decodeParams(parts[4]);
            return e;
        } catch (Exception ex) {
            System.err.println("Falha ao desserializar linha do journal: " + ex.getMessage());
            return null;
        }
    }

    private String encodeParams(Map<String,String> params) throws UnsupportedEncodingException {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String,String> en : params.entrySet()) {
            if (!first) sb.append("&");
            sb.append(URLEncoder.encode(en.getKey(), "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(en.getValue() == null ? "" : en.getValue(), "UTF-8"));
            first = false;
        }
        return sb.toString();
    }

    private Map<String,String> decodeParams(String s) throws UnsupportedEncodingException {
        Map<String,String> map = new HashMap<>();
        if (s == null || s.isEmpty()) return map;
        String[] pairs = s.split("&");
        for (String p : pairs) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) {
                String k = URLDecoder.decode(kv[0], "UTF-8");
                String v = URLDecoder.decode(kv[1], "UTF-8");
                map.put(k, v);
            }
        }
        return map;
    }

    private String escape(String s) throws UnsupportedEncodingException {
        return s == null ? "" : URLEncoder.encode(s, "UTF-8");
    }

    private String unescape(String s) throws UnsupportedEncodingException {
        return s == null ? "" : URLDecoder.decode(s, "UTF-8");
    }
}
