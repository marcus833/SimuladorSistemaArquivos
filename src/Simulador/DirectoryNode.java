package Simulador;

import java.io.Serializable;
import java.util.Map;
import java.util.LinkedHashMap;

public class DirectoryNode extends FsNode implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, FsNode> children = new LinkedHashMap<>();

    public DirectoryNode(String name) {
        super(name);
    }

    @Override
    public boolean isDirectory() { return true; }

    public Map<String, FsNode> getChildren() { return children; }

    public FsNode get(String name) { return children.get(name); }
    public void add(String name, FsNode node) {
        children.put(name, node);
        touch();
    }
    public FsNode remove(String name) {
        FsNode r = children.remove(name);
        if (r != null) touch();
        return r;
    }
    public boolean isEmpty() { return children.isEmpty(); }
}
