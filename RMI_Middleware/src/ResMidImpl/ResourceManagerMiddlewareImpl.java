package ResMidImpl;

import ResInterface.*;
import ResShadow.*;
import LockManager.*;

import java.util.*;
import java.lang.reflect.Proxy;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

//import java.rmi.RMISecurityManager;

import SocImpl.*;

public class ResourceManagerMiddlewareImpl implements ResourceManager {

	public static final int ICAR = 0;
	public static final int IFLIGHT = 1;
	public static final int IHOTEL = 2;
	public static final int ICUSTOMER = 3;

	public static ResourceManager[] remoteManagers = new ResourceManager[4];
	private static String[] remoteServers = { "Group16_CAR", "Group16_FLIGHT",
			"Group16_HOTEL", "Group16_CUSTOMER" };
	
	private static ResourceManager exportedRMObject = null;

	public static void main(String args[]) {

		// Get remote servers by hard-coding
		if (args.length == 6) {
			for (int i = 0; i < 4; i++) {
				remoteServers[i] = args[i + 2];
			}
		}

		// Switch between RMI and SMI
		if (args[0].equals("RMI")) {

			String[] server = { "localhost", "1099" };
			int port = 1099;

			if (args.length >= 2) {
				server = args[1].split(":");
				port = Integer.parseInt(server[1]);
				try {
					// get a reference to the rmiregistry
					Registry registry = LocateRegistry.getRegistry(server[0],
							port);
					for (int i = 0; i < remoteServers.length; i++) {
						// Get the proxy and the remote reference by rmiregistry
						// lookup
						remoteManagers[i] = MasterRequestingShadow.CreateShadow(
								registry, remoteServers[i],
								registry.lookup(remoteServers[i]));

						if (remoteManagers[i] != null) {
							System.out.println("Adding " + remoteServers[i]
									+ " successfully.");
						} else {
							System.out.println("Adding " + remoteServers[i]
									+ " unsuccessfully");
						}
					}

					ResourceManagerMiddlewareImpl obj = new ResourceManagerMiddlewareImpl();
					ResourceManager proxyObj = RMIHandler.newProxy(obj);
					// ResourceManager rm = (ResourceManager)
					// UnicastRemoteObject
					// .exportObject(proxyObj, 0);
					exportedRMObject = BackwardShadow.newShadow(registry, "Group16_MIDDLEWARE", proxyObj);
					// registry = LocateRegistry.createRegistry(port);
					// registry.rebind("Group16_MIDDLEWARE", rm);

					System.err.println("Middleware server ready");

				} catch (Exception e) {
					System.err.println("Server exception: " + e.toString());
					e.printStackTrace();
				}
			}
		} else if (args[0].equals("TCP")) {
			try {
				for (int i = 0; i < remoteServers.length; i++) {
					// get the proxy
					remoteManagers[i] = SocRequester
							.newRMInstance(remoteServers[i]);

					if (remoteManagers[i] != null) {
						System.out.println("Adding " + remoteServers[i]
								+ " successfully.");
					} else {
						System.out.println("Adding " + remoteServers[i]
								+ " unsuccessfully.");
					}
				}

				ResourceManagerMiddlewareImpl obj = new ResourceManagerMiddlewareImpl();
				SocListener listener = new SocListener(obj, args[1]);

				System.err.println("Middleware server ready");

			} catch (Exception e) {
				System.err.println("Server exception: " + e.toString());
				e.printStackTrace();
			}
			// try {
			// for (int i = 0; i < remoteServers.length; i++) {
			// ;
			// // get the proxy
			// remoteManagers[i] = SMIRequestor
			// .newRMInstance(remoteServers[i]);
			//
			// if (remoteManagers[i] != null) {
			// System.out.println("Successful adding "
			// + remoteServers[i]);
			// } else {
			// System.out.println("Unsuccessful adding "
			// + remoteServers[i]);
			// }
			// }
			//
			// ResourceManagerMiddlewareImpl obj = new
			// ResourceManagerMiddlewareImpl();
			// SMIListener l = new SMIListener(obj, args[1]);
			//
			// System.err.println("Master server ready");
			//
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
		} else {
			System.err
					.println("The first argument is undefined, which should be RMI or SMI");
		}
	}

	public ResourceManagerMiddlewareImpl() throws RemoteException {
		super();
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its
	// current price
	public boolean addFlight(int id, int flightNum, int flightSeats,
			int flightPrice) throws RemoteException {
		try {
			if (id == 99) {
				try { // sleep for seconds before doing addFlight
					Thread.sleep(10000);
				} catch (Exception e) {
				}
			}
			return remoteManagers[IFLIGHT].addFlight(id, flightNum,
					flightSeats, flightPrice);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean deleteFlight(int id, int flightNum) throws RemoteException {
		try {
			return remoteManagers[IFLIGHT].deleteFlight(id, flightNum);
		} catch (Exception e) {
			System.out.println("EXCEPTION:");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains
	// its current price
	public boolean addRooms(int id, String location, int count, int price)
			throws RemoteException {
		try {
			return remoteManagers[IHOTEL].addRooms(id, location, count, price);
		} catch (Exception e) {
			System.out.println("EXCEPTION:");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	// Delete rooms from a location
	public boolean deleteRooms(int id, String location) throws RemoteException {
		try {
			return remoteManagers[IHOTEL].deleteRooms(id, location);
		} catch (Exception e) {
			System.out.println("EXCEPTION:");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its
	// current price
	public boolean addCars(int id, String location, int count, int price)
			throws RemoteException {
		try {
			return remoteManagers[ICAR].addCars(id, location, count, price);
		} catch (Exception e) {
			System.out.println("EXCEPTION:");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	// Delete cars from a location
	public boolean deleteCars(int id, String location) throws RemoteException {
		try {
			return remoteManagers[ICAR].deleteCars(id, location);
		} catch (Exception e) {
			System.out.println("EXCEPTION:");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	// Returns the number of empty seats on this flight
	public int queryFlight(int id, int flightNum) throws RemoteException {
		return remoteManagers[IFLIGHT].queryFlight(id, flightNum);
	}

	// Returns the number of reservations for this flight.
	// public int queryFlightReservations(int id, int flightNum)
	// throws RemoteException
	// {
	// Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum +
	// ") called" );
	// RMInteger numReservations = (RMInteger) readData( id,
	// Flight.getNumReservationsKey(flightNum) );
	// if( numReservations == null ) {
	// numReservations = new RMInteger(0);
	// } // if
	// Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum +
	// ") returns " + numReservations );
	// return numReservations.getValue();
	// }

	// Returns price of this flight
	public int queryFlightPrice(int id, int flightNum) throws RemoteException {
		return remoteManagers[IFLIGHT].queryFlightPrice(id, flightNum);
	}

	// Returns the number of rooms available at a location
	public int queryRooms(int id, String location) throws RemoteException {
		return remoteManagers[IHOTEL].queryRooms(id, location);
	}

	// Returns room price at this location
	public int queryRoomsPrice(int id, String location) throws RemoteException {
		return remoteManagers[IHOTEL].queryRoomsPrice(id, location);
	}

	// Returns the number of cars available at a location
	public int queryCars(int id, String location) throws RemoteException {
		return remoteManagers[ICAR].queryCars(id, location);
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int id, String location) throws RemoteException {
		return remoteManagers[ICAR].queryCarsPrice(id, location);
	}

	// return a bill
	public String queryCustomerInfo(int id, int customerID)
			throws RemoteException {
		return remoteManagers[ICUSTOMER].queryCustomerInfo(id, customerID);
	}

	// customer functions
	// new customer just returns a unique customer identifier

	public int newCustomer(int id) throws RemoteException {
		return remoteManagers[ICUSTOMER].newCustomer(id);
	}

	// I opted to pass in customerID instead. This makes testing easier
	public boolean newCustomer(int id, int customerID) throws RemoteException {
		return remoteManagers[ICUSTOMER].newCustomer(id, customerID);
	}

	// Deletes customer from the database.
	public boolean deleteCustomer(int id, int customerID)
			throws RemoteException {
		String queryInfo = remoteManagers[ICUSTOMER].queryCustomerInfo(id,
				customerID);
		String[] lines = queryInfo.split("\n");

		// acquire all locks before making changes
		try {
			for (int i = 1; i < lines.length; i++) {
				String[] words = lines[i].split(" ");
				String[] swords = words[1].split("-");
				switch (words[1].charAt(0)) {
				case 'c':
					TransactionManager.Lock(ICAR, id, swords[1],
							LockManager.WRITE);
					break;
				case 'f':
					TransactionManager.Lock(IFLIGHT, id, swords[1],
							LockManager.WRITE);
					break;
				case 'r':
					TransactionManager.Lock(IHOTEL, id, swords[1],
							LockManager.WRITE);
					break;
				default:
					break;
				}
			}
		} catch (DeadlockException e) {
			e.getStackTrace();
			return false;
		}
		for (int i = 1; i < lines.length; i++) {
			String[] words = lines[i].split(" ");
			String[] swords = words[1].split("-");
			switch (words[1].charAt(0)) {
			case 'c':
				remoteManagers[ICAR].cancelReservation(id, customerID,
						words[1], swords[1], Integer.valueOf(words[0]));
				break;
			case 'f':
				remoteManagers[IFLIGHT].cancelReservation(id, customerID,
						words[1], swords[1], Integer.valueOf(words[0]));
				break;
			case 'r':
				remoteManagers[IHOTEL].cancelReservation(id, customerID,
						words[1], swords[1], Integer.valueOf(words[0]));
				break;
			default:
				break;
			}
		}
		if (remoteManagers[ICUSTOMER].deleteCustomer(id, customerID)) {
			return true;
		}
		return false;
	}

	// Frees flight reservation record. Flight reservation records help us make
	// sure we
	// don't delete a flight if one or more customers are holding reservations
	// public boolean freeFlightReservation(int id, int flightNum)
	// throws RemoteException
	// {
	// Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum +
	// ") called" );
	// RMInteger numReservations = (RMInteger) readData( id,
	// Flight.getNumReservationsKey(flightNum) );
	// if( numReservations != null ) {
	// numReservations = new RMInteger( Math.max( 0,
	// numReservations.getValue()-1) );
	// } // if
	// writeData(id, Flight.getNumReservationsKey(flightNum), numReservations );
	// Trace.info("RM::freeFlightReservations(" + id + ", " + flightNum +
	// ") succeeded, this flight now has "
	// + numReservations + " reservations" );
	// return true;
	// }
	//

	// Adds car reservation to this customer.
	public boolean reserveCar(int id, int customerID, String location)
			throws RemoteException {
		int availability = 0;
		int price = 0;
		String key = null;
		availability = remoteManagers[ICAR].queryCars(id, location);
		price = remoteManagers[ICAR].queryCarsPrice(id, location);
		key = "car-" + location.toLowerCase();
		// key.toLowerCase();
		if (availability != 0) {
			remoteManagers[ICUSTOMER].reserveInCustomerServer(id, customerID,
					key, location, price);
			remoteManagers[ICAR].reserveInItemServer(id, customerID, key,
					location);
			return true;
		} else {
			System.out.println("Cars are not available.");
			return false;
		}
	}

	// Adds room reservation to this customer.
	public boolean reserveRoom(int id, int customerID, String location)
			throws RemoteException {
		int availability = 0;
		int price = 0;
		String key = null;
		availability = remoteManagers[IHOTEL].queryRooms(id, location);
		price = remoteManagers[IHOTEL].queryRoomsPrice(id, location);
		key = "room-" + location.toLowerCase();
		// key.toLowerCase();
		if (availability != 0) {
			remoteManagers[ICUSTOMER].reserveInCustomerServer(id, customerID,
					key, location, price);
			remoteManagers[IHOTEL].reserveInItemServer(id, customerID, key,
					location);
			return true;
		} else {
			System.out.println("Rooms are not available.");
			return false;
		}
	}

	// Adds flight reservation to this customer.
	public boolean reserveFlight(int id, int customerID, int flightNum)
			throws RemoteException {
		int availablity = 0;
		int price = 0;
		String key = null;
		availablity = remoteManagers[IFLIGHT].queryFlight(id, flightNum);
		price = remoteManagers[IFLIGHT].queryFlightPrice(id, flightNum);
		key = "flight-" + String.valueOf(flightNum);
		// key.toLowerCase();
		if (availablity != 0) {
			remoteManagers[ICUSTOMER].reserveInCustomerServer(id, customerID,
					key, String.valueOf(flightNum), price);
			remoteManagers[IFLIGHT].reserveInItemServer(id, customerID, key,
					String.valueOf(flightNum));
			return true;
		} else {
			System.out.println("Flights not available.");
			return false;
		}
	}

	/* reserve an itinerary */
	public boolean itinerary(int id, int customer, Vector flightNumbers,
			String location, boolean Car, boolean Room) throws RemoteException {
		System.out.println("Hey, i am here");

		// variables
		boolean b1 = false, b2 = true, b3 = true;
		int flightPrice = 0, carPrice = 0, roomPrice = 0;
		String flightKey = null, carKey = null, roomKey = null;
		int avCar = 0, avRoom = 0, avFlight = 0;

		System.out.println("Flightnumbers: " + flightNumbers);
		for (int i = 0; i < flightNumbers.size(); i++) {
			System.out.println("Hey, i am in flight");
			avFlight = remoteManagers[IFLIGHT].queryFlight(id,
					Integer.valueOf(flightNumbers.get(i).toString()));
			flightPrice = remoteManagers[IFLIGHT].queryFlightPrice(id,
					Integer.valueOf(flightNumbers.get(i).toString()));
			flightKey = "flight-" + flightNumbers.get(i).toString();
			flightKey.toLowerCase();
			if (avFlight != 0) {
				b1 = remoteManagers[ICUSTOMER].reserveInCustomerServer(id,
						customer, flightKey, flightNumbers.get(i).toString(),
						flightPrice)
						&& remoteManagers[IFLIGHT].reserveInItemServer(id,
								customer, flightKey, flightNumbers.get(i)
										.toString());
				if (b1) {
					System.out.println("b1 is true");
				} else {
					System.out.println("b1 is false");
				}
			} else {
				System.out.println("Flight not available.");
				return false;
			}
		}

		System.out.println("Car: " + Car);
		if (Car) {
			System.out.println("Hey, i am in Car");
			avCar = remoteManagers[ICAR].queryCars(id, location);
			carPrice = remoteManagers[ICAR].queryCarsPrice(id, location);
			carKey = "car-" + location.toLowerCase();
			// carKey = carKey.toLowerCase();
			System.out.println("Car key:" + carKey);
			if (avCar != 0) {
				b2 = remoteManagers[ICUSTOMER].reserveInCustomerServer(id,
						customer, carKey, location, carPrice)
						&& remoteManagers[ICAR].reserveInItemServer(id,
								customer, carKey, location);
				if (b2) {
					System.out.println("b2 is true");
				} else {
					System.out.println("b2 is false");
				}
			} else {
				System.out.println("Car not available.");
				return false;
			}
		}

		System.out.println("Room: " + Room);
		if (Room) {
			System.out.println("Hey, i am in Room");
			avRoom = remoteManagers[IHOTEL].queryRooms(id, location);
			roomPrice = remoteManagers[IHOTEL].queryRoomsPrice(id, location);
			roomKey = "room-" + location.toLowerCase();
			// roomKey = roomKey.toLowerCase();
			System.out.println("Room key:" + roomKey);
			if (avRoom != 0) {
				b3 = remoteManagers[ICUSTOMER].reserveInCustomerServer(id,
						customer, roomKey, location, roomPrice)
						&& remoteManagers[IHOTEL].reserveInItemServer(id,
								customer, roomKey, location);
				if (b3) {
					System.out.println("b3 is true");
				} else {
					System.out.println("b3 is false");
				}
			} else {
				System.out.println("Room not available.");
				return false;
			}
		}

		if (b1 && b2 && b3) {
			System.out.println();
			System.out.println("Congratulations!");
			System.out
					.println("The total plan has been reserved successfully!");
			return true;
		}
		System.out.println("Unable to complete your operations.");
		return false;
	}

	@Override
	public boolean reserveInCustomerServer(int id, int customerID, String key,
			String location, int price) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reserveInItemServer(int id, int customerID, String key,
			String location) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean cancelReservation(int id, int customerID, String key,
			String location, int itemNum) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int start() throws RemoteException {
		return TransactionManager.start();
	}

	@Override
	public boolean commit(int transactionId) throws RemoteException,
			TransactionAbortedException, InvalidTransactionException {
		return TransactionManager.commit(transactionId);
	}

	@Override
	public void abort(int transactionId) throws RemoteException,
			InvalidTransactionException {
		TransactionManager.abort(transactionId);
	}

	@Override
	public boolean shutdown() throws RemoteException {
		if (TransactionManager.TimerTable.size() > 0) {
			return false;
		}

		for (int i = 0; i < remoteManagers.length; i++) {
			try {
				remoteManagers[i].shutdown();
			} catch (RemoteException e) {
				System.out.println(e.getMessage());
			}
		}
		System.exit(0);

		return true;
	}

	@Override
	public byte[][] outboundSync() throws RemoteException {
		return TransactionManager.outboundSync();
	}

	@Override
	public void inboundSync(byte[][] data) throws RemoteException {
		System.out.println("sihdshflsdhfjkhdsj");
		TransactionManager.inboundSync(data);
	}

	@Override
	public void promote() throws RemoteException {
		// not supported, promote handled in shadow
	}
}
