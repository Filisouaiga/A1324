/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package common;

import java.io.Serializable;

/**
 *
 * @author jeral
 */
public class WorkerInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String host;
    private final int port;
    private final String rmiName;

    public WorkerInfo(int id, String host, int port, String rmiName) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.rmiName = rmiName;
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getRmiName() {
        return rmiName;
    }

    /** Full RMI lookup URL for this worker */
    public String getRmiUrl() {
        return "rmi://" + host + ":" + port + "/" + rmiName;
    }

    @Override
    public String toString() {
        return "WorkerInfo{id=" + id + ", host=" + host + ", port=" + port + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkerInfo)) return false;
        return id == ((WorkerInfo) o).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
