package sequencer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Registry {
    public Registry() {
        System.out.println("RMI registry server started");

        try { // special exception handler for registry creation
            LocateRegistry.createRegistry(1099);
            System.out.println("RMI registry created.");
        } catch (RemoteException e) {
            // do nothing, error means registry already exists
            System.out.println("java RMI registry already exists.");
        }
    }
}
