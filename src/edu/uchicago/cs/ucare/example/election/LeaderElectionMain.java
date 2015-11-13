package edu.uchicago.cs.ucare.example.election;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uchicago.cs.ucare.example.election.interposition.LeaderElectionInterposition;
import edu.uchicago.cs.ucare.samc.election.LeaderElectionAspectProperties;
import edu.uchicago.cs.ucare.samc.election.LeaderElectionPacket;
import edu.uchicago.cs.ucare.samc.server.ModelCheckingServer;
import edu.uchicago.cs.ucare.samc.util.LeaderElectionLocalState;
import edu.uchicago.cs.ucare.samc.util.PacketReceiveAck;

public class LeaderElectionMain {
	
    private static final Logger LOG = LoggerFactory.getLogger(LeaderElectionMain.class);
    
	public static final int LOOKING = 0;
	public static final int FOLLOWING = 1;
	public static final int LEADING = 2;
	
	public static String getRoleName(int role) {
		String name;
		switch (role) {
		case LeaderElectionMain.LOOKING:
			name = "looking";
			break;
		case LeaderElectionMain.FOLLOWING:
			name = "following";
			break;
		case LeaderElectionMain.LEADING:
			name = "leading";
			break;
		default:
			name = "unknown";
			break;
		}
		return name;
	}
	
	public static int id;
	public static int role;
	public static int leader;
	
	public static Map<Integer, InetSocketAddress> nodeMap;
	public static Map<Integer, Sender> senderMap;
	public static Processor processor;
	
	public static Map<Integer, Integer> electionTable;
	
	public static void readConfig(String config) throws IOException {
		nodeMap = new HashMap<Integer, InetSocketAddress>();
		BufferedReader br = new BufferedReader(new FileReader(config));
		String line;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.trim().split("=");
			assert tokens.length == 2;
			int nodeId = Integer.parseInt(tokens[0]);
			String[] inetSocketAddress = tokens[1].split(":");
			assert inetSocketAddress.length == 2;
			InetSocketAddress nodeAddress = new InetSocketAddress(inetSocketAddress[0], Integer.parseInt(inetSocketAddress[1]));
			nodeMap.put(nodeId, nodeAddress);
			LOG.info("node " + nodeId + " is " + nodeAddress);
		}
		LOG.info("Cluster size = " + nodeMap.size());
		br.close();
	}
	
	public static void work() throws IOException {
	    if (LeaderElectionInterposition.SAMC_ENABLED) {
            LeaderElectionInterposition.numNode = nodeMap.size();
            LeaderElectionInterposition.isReading = new boolean[LeaderElectionInterposition.numNode];
            Arrays.fill(LeaderElectionInterposition.isReading, false);
	    }
		senderMap = new HashMap<Integer, Sender>();
		InetSocketAddress myAddress = nodeMap.get(id);
		processor = new Processor();
		processor.start();
        final ServerSocket server = new ServerSocket(myAddress.getPort());
        Thread listeningThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
		            Socket connection;
					try {
						connection = server.accept();
                        DataInputStream dis = new DataInputStream(connection.getInputStream());
                        DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                        int otherId = dis.readInt();
                        LOG.info("connection from " + otherId);
                        boolean isAllowed = otherId > id;
                        dos.writeBoolean(isAllowed);
                        dos.flush();
                        if (!isAllowed) {
                        	LOG.info("connection from " + otherId + " is not allowed");
                            connection.close();
                        } else {
                            Sender sender = new Sender(otherId, connection);
                            senderMap.put(otherId, sender);
                            sender.start();
                            Receiver receiver = new Receiver(otherId, connection);
                            receiver.start();
                        }
					} catch (IOException e) {
						// TODO Auto-generated catch block
						LOG.error("", e);
						break;
					}
				}
				try {
					server.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					LOG.error("", e);
				}
			}
        	
        });
        listeningThread.start();
        try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			LOG.error("", e1);
		}
        // Sleep to make sure that every node is running now
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            LOG.error("", e1);
        }
        // TODO Auto-generated method stub
        for (Integer nodeId : nodeMap.keySet()) {
            if (nodeId != id) {
                InetSocketAddress address = nodeMap.get(nodeId);
                try {
                    LOG.info("Connecting to " + nodeId);
                    Socket connect = new Socket(address.getAddress(), address.getPort());
                    DataOutputStream dos = new DataOutputStream(connect.getOutputStream());
                    dos.writeInt(id);
                    dos.flush();
                    DataInputStream dis = new DataInputStream(connect.getInputStream());
                    boolean isAllowed = dis.readBoolean();
                    if (isAllowed) {
                        LOG.info("Connecting to " + nodeId + " is allowed");
                        Sender sender = new Sender(nodeId, connect);
                        senderMap.put(nodeId, sender);
                        sender.start();
                        Receiver receiver = new Receiver(nodeId, connect);
                        receiver.start();
                    } else {
                        LOG.info("Connecting to " + nodeId + " is not allowed");
                        connect.close();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    LOG.error("", e);
                }
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            LOG.error("", e);
        }
        LOG.info("First send all " + senderMap);
        processor.sendAll(getCurrentMessage());
        if (LeaderElectionInterposition.SAMC_ENABLED) {
            LeaderElectionInterposition.firstSent = true;
        }

        if (LeaderElectionInterposition.SAMC_ENABLED) {
            LeaderElectionInterposition.bindCallback();
            LeaderElectionInterposition.isBound = true;
            if (LeaderElectionInterposition.isReadingForAll() && !LeaderElectionInterposition.isThereSendingMessage() && LeaderElectionInterposition.isBound) {
                try {
                    LeaderElectionInterposition.modelCheckingServer.informSteadyState(id, 0);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
	}
	
	static boolean isBetterThanCurrentLeader(ElectionMessage msg) {
		return msg.leader > leader;
	}
	
	static int isFinished() {
		int totalNode = nodeMap.size();
		Map<Integer, Integer> count = new HashMap<Integer, Integer>();
		for (Integer electedLeader : electionTable.values()){
			count.put(electedLeader, count.containsKey(electedLeader) ? count.get(electedLeader) + 1 : 1);
		}
		LOG.info("Election table " + electionTable);
		LOG.info("Count table " + count);
		for (Integer electedLeader : count.keySet()) {
			int totalElect = count.get(electedLeader);
			if (totalElect > totalNode / 2) {
				return electedLeader;
			}
		}
		return -1;
	}
	
	static ElectionMessage getCurrentMessage() {
		return new ElectionMessage(id, role, leader);
	}
	
	public static void main(String[] args) throws IOException {

		if (args.length != 2) {
			System.err.println("usage: LeaderElectionMain <id> <config>");
			System.exit(1);
		}
		
		if (LeaderElectionInterposition.SAMC_ENABLED) {
            LOG.info("Enable SAMC");
		}
		LeaderElectionInterposition.localState = new LeaderElectionLocalState();
		
		id = Integer.parseInt(args[0]);
		role = LOOKING;
		leader = id;
		
		LOG.info("Started:my id = " + id + " role = " + getRoleName(role) + " " + " leader = " + leader);
		
        electionTable = new HashMap<Integer, Integer>();
        electionTable.put(id, leader);

		if (LeaderElectionInterposition.SAMC_ENABLED) {
		    LeaderElectionInterposition.id = id;
            LeaderElectionInterposition.localState.setRole(role);
            LeaderElectionInterposition.localState.setLeader(leader);
            LeaderElectionInterposition.localState.setElectionTable(electionTable);
			LeaderElectionInterposition.modelCheckingServer.setLocalState(id, LeaderElectionInterposition.localState);
			LeaderElectionInterposition.modelCheckingServer.updateLocalState(id, LeaderElectionInterposition.localState.hashCode());
		}

		readConfig(args[1]);
		work();
		
	}
	
	public static class Receiver extends Thread {
		
		public int otherId;
		public Socket connection;

		public Receiver(int otherId, Socket connection) {
			this.otherId = otherId;
			this.connection = connection;
		}
		
		void read(DataInputStream dis, byte[] buffer) throws IOException {
			int alreadyRead = 0;
            while (alreadyRead != ElectionMessage.SIZE) {
                alreadyRead = dis.read(buffer, alreadyRead, ElectionMessage.SIZE - alreadyRead);
            }
		}
		
		@Override
		public void run() {
			LOG.info("Start receiver for " + otherId);
			try {
				DataInputStream dis = new DataInputStream(connection.getInputStream());
                byte[] buffer = new byte[ElectionMessage.SIZE];
                while (!connection.isClosed()) {
                	LOG.info("Reading message for " + otherId);

                	if (LeaderElectionInterposition.SAMC_ENABLED) {
                	    LeaderElectionInterposition.isReading[this.otherId] = true;
                        if (LeaderElectionInterposition.isReadingForAll() 
                                && !LeaderElectionInterposition.isThereSendingMessage() 
                                && LeaderElectionInterposition.isBound) {
                            try {
                                LeaderElectionInterposition.modelCheckingServer.informSteadyState(id, 0);
                            } catch (RemoteException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                	}
                	
                	read(dis, buffer);

                	if (LeaderElectionInterposition.SAMC_ENABLED) {
                        LeaderElectionInterposition.isReading[this.otherId] = false;
                	}
                	
                    ElectionMessage msg = new ElectionMessage(otherId, buffer);
                    LOG.info("Get message : " + msg.toString());
                    if (LeaderElectionInterposition.SAMC_ENABLED) {
                        LeaderElectionPacket packet = LeaderElectionInterposition.packetGenerator2
                                .createNewLeaderElectionPacket("LeaderElectionCallback" + id, 
                                msg.getSender(), id, msg.getRole(), msg.getLeader());
                        try {
                            LeaderElectionInterposition.ack.ack(packet.getId(), id);
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } 
                    }
                    processor.process(msg);
                }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOG.error("", e);
			}
		}
		
	}
	
	public static class Sender extends Thread {
		
		public int otherId;
		public Socket connection;
		public LinkedBlockingQueue<ElectionMessage> queue;
		public OutputStream os;
		
		public Sender(int otherId, Socket connection) {
			this.otherId = otherId;
			this.connection = connection;
			try {
				os = connection.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOG.error("", e);
			}
			queue = new LinkedBlockingQueue<ElectionMessage>();
		}
		
		public void send(ElectionMessage msg) {
			try {
				queue.put(msg);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				LOG.error("", e);
			}
		}
		
		public synchronized void write(ElectionMessage msg) {
            try {
				os.write(msg.toBytes());
                os.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOG.error("", e);
			}
		}
		
		@Override
		public void run() {
			LOG.info("Start sender for " + otherId);
            while (!connection.isClosed()) {
                try {
                    ElectionMessage msg = queue.take();
                    LOG.info("Send message : " + msg.toString() + " to " + otherId);
                    if (LeaderElectionInterposition.SAMC_ENABLED) {
                        try {
                            LeaderElectionPacket packet = new LeaderElectionPacket("LeaderElectionCallback" + id);
                            packet.addKeyValue(LeaderElectionPacket.EVENT_ID_KEY, LeaderElectionInterposition.hash(msg, this.otherId));
                            packet.addKeyValue(LeaderElectionPacket.SOURCE_KEY, id);
                            packet.addKeyValue(LeaderElectionPacket.DESTINATION_KEY, this.otherId);
                            packet.addKeyValue(LeaderElectionPacket.LEADER_KEY, msg.getLeader());
                            packet.addKeyValue(LeaderElectionPacket.ROLE_KEY, msg.getRole());
                            LeaderElectionInterposition.nodeSenderMap.put(packet.getId(), packet);
                            LeaderElectionInterposition.msgSenderMap.put(packet.getId(), this);
                            try {
                                LeaderElectionInterposition.modelCheckingServer.offerPacket(packet);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            LOG.error("", e);
                        }
                    } else {
                        write(msg);
                    }
                    if (LeaderElectionInterposition.SAMC_ENABLED) {
                        if (LeaderElectionInterposition.isReadingForAll() 
                                && !LeaderElectionInterposition.isThereSendingMessage() 
                                && LeaderElectionInterposition.isBound) {
                            try {
                                LeaderElectionInterposition.modelCheckingServer.informSteadyState(id, 0);
                            } catch (RemoteException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    LOG.error("", e);
                }
            }
		}

		public int getOtherId() {
			return otherId;
		}

		public void setOtherId(int otherId) {
			this.otherId = otherId;
		}
		
	}
	
	public static class Processor extends Thread {
		
		LinkedBlockingQueue<ElectionMessage> queue;
		
		public Processor() {
			queue = new LinkedBlockingQueue<ElectionMessage>();
		}
		
		public void process(ElectionMessage msg) {
			queue.add(msg);
		}
		
		public void sendAll(ElectionMessage msg) {
			LOG.info("Sender map " + senderMap);
			for (Integer nodeId : senderMap.keySet()) {
				if (nodeId != id) {
					senderMap.get(nodeId).send(msg);
				}
			}
		}
		
		@Override
		public void run() {
			LOG.info("Start processor");
			ElectionMessage msg;
			while (true) {
				try {
					LOG.info("Current role is " + 
							(role == LEADING ? "Leading" : role == FOLLOWING ? "Following" : "Looking") + 
							"; current leader is " + leader);
					msg = queue.take();
					LOG.info("Process message : " + msg.toString());
                    electionTable.put(msg.getSender(), msg.getLeader());
					switch (role) {
					case LOOKING:
						switch (msg.getRole()) {
						case LOOKING:
							if (isBetterThanCurrentLeader(msg)) {
								LOG.info("Message " + msg + " is better");
								leader = msg.getLeader();
								electionTable.put(id, leader);
								int newLeader = isFinished();
								LOG.info("New leader = " + newLeader);
								if (newLeader != -1) {
									LOG.info("Finished election, leader = " + newLeader);
									if (newLeader == id) {
										role = LEADING;
									} else {
										role = FOLLOWING;
									}
								}
								if (LeaderElectionInterposition.SAMC_ENABLED) {
						            LeaderElectionInterposition.localState.setRole(role);
						            LeaderElectionInterposition.localState.setLeader(leader);
						            try {
                                        LeaderElectionInterposition.modelCheckingServer.setLocalState(id, LeaderElectionInterposition.localState);
                                        LeaderElectionInterposition.modelCheckingServer.updateLocalState(id, LeaderElectionInterposition.localState.hashCode());
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
						        }
                                sendAll(getCurrentMessage());
							}
							break;
						case FOLLOWING:
						case LEADING:
							leader = msg.getLeader();
							electionTable.put(id, leader);
							int newLeader = isFinished();
                            LOG.info("Believe new leader = " + newLeader);
							if (newLeader != -1) {
								if (newLeader == id) {
									role = LEADING;
								} else {
									role = FOLLOWING;
								}
							}
							if (LeaderElectionInterposition.SAMC_ENABLED) {
                                LeaderElectionInterposition.localState.setRole(role);
                                LeaderElectionInterposition.localState.setLeader(leader);
                                try {
                                    LeaderElectionInterposition.modelCheckingServer.setLocalState(id, LeaderElectionInterposition.localState);
                                    LeaderElectionInterposition.modelCheckingServer.updateLocalState(id, LeaderElectionInterposition.localState.hashCode());
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                            sendAll(getCurrentMessage());
							break;
						}
						break;
					case FOLLOWING:
					case LEADING:
						switch (msg.getRole()) {
						case LOOKING:
							sendAll(getCurrentMessage());
							break;
						case FOLLOWING:
						case LEADING:
							// NOTE assume that conflict leader never happen
							break;
						}
						break;
					}
					LOG.info("Finished processing " + msg);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					LOG.error("", e);
				}
			}
		}
		
	}

}
