package Simulador;

import java.io.Serializable;

public class FileNode extends FsNode implements Serializable {
    private static final long serialVersionUID = 1L;
    private StringBuilder content = new StringBuilder();

    public FileNode(String name) {
        super(name);
    }

    @Override
    public boolean isDirectory() { return false; }

    public String read() { return content.toString(); }
    public void write(String data) {
        content.setLength(0);
        content.append(data);
        touch();
    }
    public void append(String data) {
        content.append(data);
        touch();
    }
}
