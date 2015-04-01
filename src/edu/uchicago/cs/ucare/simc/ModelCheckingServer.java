package edu.uchicago.cs.ucare.simc;

import java.rmi.Remote;
import java.rmi.RemoteException;

import mc.DiskWrite;
import mc.InterceptPacket;

public interface ModelCheckingServer extends Remote {
    
    public void registerCallback(int id, String callbackName) throws RemoteException;
    
    public void offerPacket(InterceptPacket packet) throws RemoteException;
    public boolean waitPacket(int toId) throws RemoteException;
    
    public void requestWrite(DiskWrite write) throws RemoteException;

    // Just for debugging, don't use this for real model checking
    public void requestWriteImmediately(DiskWrite write) throws RemoteException;

    public void setTestId(int testId) throws RemoteException;
    public void updateLocalState(int nodeId, int state) throws RemoteException;
    public void recordCodeTrace(int nodeId, int stackTraceHash) throws RemoteException;
    public void recordProtocol(int nodeId, int protocolHash) throws RemoteException;

}
