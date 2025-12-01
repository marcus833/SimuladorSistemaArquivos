package Simulador;

import java.io.Serializable;

public abstract class FsNode implements Serializable {
    private static final long serialVersionUID = 1L;
    protected String name;
    protected long createdAt;
    protected long modifiedAt;

    public FsNode(String name) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = this.createdAt;
    }

    public String getName() { return name; }
    public void setName(String n) { name = n; touch(); }
    public long getModifiedAt() { return modifiedAt; }
    public void touch() { modifiedAt = System.currentTimeMillis(); }
    public abstract boolean isDirectory();
}
