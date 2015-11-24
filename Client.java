import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;


public class Client implements Comparable {
	private String address;
	public boolean visited;
	public TreeMap<Client, Float> inTransactions; 
	public TreeMap<Client, Float> outTransactions;
	
	public float pageRank;
	public float incomingPageRank;
	public float totalSpent;
	
	
	Client(String address){
		this.address = address;
		inTransactions = new TreeMap<Client, Float>();
		outTransactions = new TreeMap<Client, Float>();
		pageRank=0;
		incomingPageRank=1;
		totalSpent=0;
		visited = false;
	}
	
	public void addOutput(Client seller, float edgeWeight){
		if (outTransactions.containsKey(seller)){
			outTransactions.put(seller, outTransactions.get(seller)+edgeWeight);
		} else{
			outTransactions.put(seller, edgeWeight);
		}
		totalSpent+=edgeWeight;
	}
	
	public void addIntput(Client seller, float value){
		if (inTransactions.containsKey(seller)){
			inTransactions.put(seller, inTransactions.get(seller)+value);
		} else{
			inTransactions.put(seller, value);
		}
	}
	
	public String toString(){
		return address;
	}                    
	
	public String outTransactions(){
		String answer = address+" sent\n";
		for (Client c: outTransactions.keySet()){
			answer+=outTransactions.get(c)+"\t to: "+ c.toString()+"\n";	
		}
		return answer;
	}
	
	public String inTransactions(){
		String answer = address+" recieves\n";
		for (Client c: inTransactions.keySet()){
			answer+=inTransactions.get(c)+"\t from: "+ c.toString()+"\n";	
		}
		return answer;
	}
	
	public void recievePR(float f){
		incomingPageRank+=f;
	}
	
	public void givePR(float dampeningfactor, int N){
		pageRank+= incomingPageRank;
		incomingPageRank=0;
		for (Client c: outTransactions.keySet()){
			c.recievePR(pageRank*outTransactions.get(c)/totalSpent);
			pageRank=0;
		}
	}

	@Override
	public int compareTo(Object o) {
		Client c = (Client) o;
		return this.toString().compareTo(c.toString());
	}
	
	public TreeMap<Client, Float> getInTransactions(){
		return inTransactions;
	}
	
	public TreeMap<Client, Float> getOutTransactions(){
		return outTransactions;
	}
	
}
