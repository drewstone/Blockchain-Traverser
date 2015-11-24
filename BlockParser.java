import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.utils.BlockFileLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;  
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class BlockParser {

	NetworkParameters np = new MainNetParams();
	List<File> blockChainFiles;
	BlockFileLoader blockLoader;
	Map<String, Client> graph;
	List<Client> clients;
	
	Queue<Client> toConsider;
	Map<Client, Set<Client>> components;
	Stack<Client> kosarajuStack;
	static FileWriter fw;//To write data to as we tested various aspects

	public BlockParser() {
		blockChainFiles = new ArrayList<File>();
		addFiles();     		//adds block chain files
		createFileWriter();
		buildData();
	}
	
	public void addFiles() {
		blockChainFiles.add(new File("blk00000.dat"));
		blockChainFiles.add(new File("blk00001.dat"));
		blockChainFiles.add(new File("blk00002.dat"));
		blockChainFiles.add(new File("blk00003.dat"));
		blockChainFiles.add(new File("blk00004.dat"));
		blockChainFiles.add(new File("blk00005.dat"));
		blockLoader = new BlockFileLoader(np, blockChainFiles);
	}
	
	/**
	 * Skims the blockchain, finding blocks that have meaningful transactions in them. 	
	 */
	public void buildData() {
		graph = new HashMap<String, Client>();
		int i = 1; 		
		while (blockLoader.hasNext() && i<20000){// the number of blocks to look at
			Block block = blockLoader.next(); 
			if (block.getTransactions().size()>1){ 
				//if a block has more transactions than just a mining operation, get transactions
				getTransactionsFromBlock(block.getHash().toString()); //pulls data from web
			}
			i++;
			if (i%100==0) System.out.println(i);
		}
		System.out.println("done");
	}
	
	/**
	 * Uses blockexporer.com to find the transactions in an easily parsed form. This was 
	 * easier, though slower, than actually reading from the blockchain locally. 
	 * @param blockHash
	 */
	public void getTransactionsFromBlock(String blockHash){
		try {
			Document doc = Jsoup.connect("http://blockexplorer.com/block/"+ blockHash).get();
			Elements links = doc.getElementsByTag("tr");
			List<Element> link_list = links.subList(2, links.size());
			for (Element link : link_list) {
				int count = 1;
				List<String> inputAddresses = new ArrayList<String>();
				List<Float> inputValues = new ArrayList<Float>();
				List<String> outputAddresses = new ArrayList<String>();
				List<Float> outValues = new ArrayList<Float>();
				long totalValue = 0;
				for (Element llink : link.getElementsByTag("td")) {
					for (Element newLink : llink.getElementsByTag("li")) {
						if (count % 4 == 0) {
							if (newLink.text().contains("Generation")) {
								continue;//these are mining operations, which we ignore
							}
							String[] parsedData = newLink.text().split(":");
							String addressCode = parsedData[0];
							float sendingAmount = Float.parseFloat(parsedData[1].trim());
							inputAddresses.add(addressCode);
							inputValues.add(sendingAmount);
						} else if (count % 5 == 0 && !inputAddresses.isEmpty()) {
							String[] parsedData = newLink.text().split(":");
							String addressCode = parsedData[0];
							Float receivingAmount = Float.parseFloat(parsedData[1].trim());
							outputAddresses.add(addressCode);
							outValues.add(receivingAmount);
							totalValue+=receivingAmount;
						}
					}
					count++;
				}
				addTransaction(inputAddresses, inputValues, outputAddresses, outValues, totalValue);
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds a single transactions to the graph. 
	 * Takes a list of Client addresses, and a list of how much each of them spent on a transaction, 
	 * as well the recipients and receiving amounts. Adds new clients to the graph, and adds edges. 
	 * @param inputs
	 * @param inputAmounts
	 * @param outputs
	 * @param outputAmounts
	 * @param val
	 */
	public void addTransaction(List<String> inputs, List<Float> inputAmounts, 
			List<String> outputs, List<Float> outputAmounts, long val){
		assert (inputs.size()==inputAmounts.size() && outputs.size()==outputAmounts.size());
		List<Client> outputClients = new ArrayList<Client>();
		for (String output: outputs){
			if (graph.containsKey(output)){
				outputClients.add(graph.get(output));
			}else{
				Client newClient = new Client(output);
				outputClients.add(newClient);
				graph.put(output, newClient);
			}
		}
		for (int i=0; i<inputs.size(); i++){
			String input = inputs.get(i);
			Client inputClient;
			if (graph.containsKey(input)){
				inputClient=graph.get(input);
			}else{
				inputClient = new Client(input);
				graph.put(input, inputClient);
			}
			addEdges(inputClient, inputAmounts.get(i), outputClients, outputAmounts, val);
		}
	}
	
	/**
	 * For a given transaction, and client, adds the apropriate edges from that client to the
	 * recipients.
	 */
	public void addEdges(Client sender, float senderAmount, List<Client> recievers, 
			List<Float> outputAmounts, float tranTotal){
		for (int i=0; i<recievers.size(); i++){
			float edgeWeight = senderAmount*outputAmounts.get(i)/tranTotal;
			sender.addOutput(recievers.get(i), edgeWeight);
			recievers.get(i).addIntput(sender, edgeWeight);
		}
	}
	
	/**
	 * Creates a file writer on every run in order to easily write the data we are pulling
	 */
	public void createFileWriter(){
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		try {
			fw = new FileWriter("output"+sdf.format(cal.getTime())+".txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * A 
	 * @param cycles
	 * @param dampeningFactor
	 * @throws IOException
	 */
	public void pageRankIterator(int cycles, float dampeningFactor) throws IOException{
		Collection<Client> clients = graph.values();
		for (int i=0; i<cycles; i++){
			for (Client c: clients){
				c.givePR(dampeningFactor, clients.size());
			}
		}
		float averageRank =0;
		for (Client c: clients){
			c.pageRank+=c.incomingPageRank;
			averageRank+=c.pageRank;
		}
		averageRank= averageRank/clients.size();
		fw.write("Average Pr is: "+ averageRank+"\n");
		System.out.println("Average Pr is: "+ averageRank);
		for (Client c: clients){
			if (c.pageRank>averageRank){
				fw.write(String.valueOf(c.pageRank)+"\n");
				System.out.println(String.valueOf(c.pageRank));
			}
		}
		fw.write("\n\n\n\n\n\n");
		for (Client c: clients){
			if (c.pageRank>averageRank){
				fw.write(String.valueOf(c.pageRank)+"\t"+c.toString());
				System.out.println(String.valueOf(c.pageRank)+"\t"+c.toString()+"\t"+ c.totalSpent);
			}
		}

		
	}
	
	
	public Set<Collection<Client>> findWeaklyConnectedComponents(){
		components = new HashMap<Client, Set<Client>>();
		toConsider = new ConcurrentLinkedQueue<Client>();
		for (Client c: graph.values()){
			toConsider.add(c);
		}
		while (!toConsider.isEmpty()){
			Client c = toConsider.remove();
			Set<Client> component = new TreeSet<Client>();
			component.add(c);
			weaklyConnectedComponent(c, component);
			components.put(c, component);
		}
		Set<Collection<Client>> weaklyCCs = new HashSet<Collection<Client>>();
		for (Set<Client> cc: components.values()){
			weaklyCCs.add(cc);
		}
		return weaklyCCs;
		
	}

	public Set<Client> weaklyConnectedComponent(Client c, Set<Client> component){
		for (Client in: c.getInTransactions().keySet()){
			if (!component.contains(in)){
				components.put(in, component);
				component.add(in);
				weaklyConnectedComponent(in, component);
				toConsider.remove(in);
			}
		}
		for (Client out: c.getOutTransactions().keySet()){
			if (!component.contains(out)){
				components.put(out, component);
				component.add(out);
				weaklyConnectedComponent(out, component);
				toConsider.remove(out);
			}
		}
		return component;
	}
	
	
	
	public Map<Client, Set<Client>> findStronglyConnectComponents(){
		kosarajuStack = new Stack<Client>();
		toConsider = new ConcurrentLinkedQueue<Client>();
		for (Client c: graph.values()){
			toConsider.add(c);
		}
		while (!toConsider.isEmpty()){
			Client c = toConsider.remove();
			dfsKosaraju(c);
		}
		for (Client c: graph.values()){
			toConsider.add(c);
		}
		Map<Client, Set<Client>> stronglyConnected = new HashMap<Client, Set<Client>>();
		while (!kosarajuStack.isEmpty()){
			Client considering = kosarajuStack.pop();
			Set<Client> component = new TreeSet<Client>();
			component.add(considering);
			stronglyConnected.put(considering,component);
			for (Client inEdge: considering.inTransactions.keySet()){
				if (toConsider.contains(inEdge)){
					component.add(inEdge);
					toConsider.remove(inEdge);
					kosarajuStack.remove(inEdge);
					stronglyConnected.put(inEdge, component);
				}
			}
		}
		return stronglyConnected;
	}	

	public void dfsKosaraju(Client c){
		toConsider.remove(c);
		for (Client outEdge: c.getOutTransactions().keySet()){
			if (toConsider.contains(outEdge)){
				dfsKosaraju(outEdge);
			}
		}
		kosarajuStack.push(c);
	}
	
	
	public void DFS(Client c, List<Client> list, boolean out){
		c.visited=true;
		Collection<Client> edges;
		if (out){
			edges = c.getOutTransactions().keySet();
		}else{
			edges = c.getInTransactions().keySet();
		}
		for (Client outEdge: edges){
			if (!outEdge.visited){
				DFS(outEdge, list, out);
			}
		}
		list.add(c);
	}
	
	public ArrayList<Client> fillOrder(){
		ArrayList<Client> stack = new ArrayList<Client>();
		for (Client c: graph.values()){
			if (!c.visited){
				DFS(c, stack, true);
			}
		}
		return stack;
	}
	
	public Set<Collection<Client>> getStronglyConnectedComponent(){
		ArrayList<Client> firstOrder = fillOrder();
		Collections.reverse(firstOrder);
		resetVisited();
		Set<Collection<Client>> ssc = new HashSet<Collection<Client>>();
		for (Client c: firstOrder){
			if (!c.visited){
				List<Client> component = new ArrayList<Client>();
				DFS(c, component, false);
				ssc.add(component);
			}
		}
		return ssc;
	}
	
	public void resetVisited(){
		for (Client c: graph.values()){
			c.visited=false;
		}
	}

	public static void analyzeComponents(Map<Client, Set<Client>> components){
		System.out.println(components.keySet().size());
		float averageSize = components.keySet().size()/components.values().size();
		System.out.println("Average Size is: "+averageSize);
		averageSize = 1;
		Set<Set<Client>> bigComponents = new HashSet<Set<Client>>();
		averageSize=0;
		for (Set<Client> component: components.values()){
			if (component.size()>averageSize){
				bigComponents.add(component);
			}
		}
		for (Set<Client> component: bigComponents){
			System.out.println("Size is: "+component.size());
			for (Client c: component){
				System.out.print(c+"\t");
			}
			System.out.println("\n");
			averageSize+=component.size();
		}
		averageSize /=bigComponents.size();
		System.out.println("Average Size is: "+averageSize);
	}
	
	public static void analyzeComponents(Set<Collection<Client>> components) throws IOException{
		float averageSize = 0;
		for (Collection<Client> clients: components){
			averageSize+=clients.size();
		}
		averageSize=averageSize/components.size();
		fw.write ("Average Size is: "+averageSize+"\n");
		System.out.println("Average Size is: "+averageSize);
		for (Collection<Client> component: components){
			if (component.size()>1){
				fw.write("Size is: "+component.size()+"\n");
				System.out.println("Size is: "+component.size());
				for (Client c: component){
					//fw.write(c+"\t");
					System.out.print(c+"\t");
				}
				//fw.write("\n");
				System.out.println("");
			}
		}
	}
	
	public static void main(String args[]) {
		BlockParser blockParser = new BlockParser();
		System.out.println("Done Making Graph");
		//blockParser.pageRankIterator(10,(float)0.6);
		try {
			//analyzeComponents(blockParser.findWeaklyConnectedComponents());
			//analyzeComponents(blockParser.getStronglyConnectedComponent());
			blockParser.pageRankIterator(10, (float) 1);
			blockParser.fw.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//analyzeComponents(blockParser.findWeaklyConnectedComponents());
	}
}