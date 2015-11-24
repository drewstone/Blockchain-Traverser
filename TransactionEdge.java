import org.bitcoinj.core.Coin;


public class TransactionEdge {
	private Client sender;
	private Client reciever;
	private long amount;
	
	TransactionEdge(Client sender, Client reciever, Coin amount){
		this.sender=sender;
		this.reciever=reciever;
		this.amount=amount.value;
	}
	
	public void addWeight(Coin amount){
		this.amount+=amount.value;
	}
	

}
