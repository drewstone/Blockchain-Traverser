import java.util.List;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;


public class BlockNode {

	private Block currentBlock;
	private Sha256Hash previousBlock;
	private Block nextBlock;
	private Sha256Hash hashCode;
	
	public BlockNode(Block block) {
		currentBlock = block;
		hashCode = block.getHash();
	}
	
	public String toString() {
		return currentBlock.toString();
	}
	
	public List<Transaction> getTransactions() {
		return currentBlock.getTransactions();
	}
	
	public void setNextBlock(Block block) {
		
	}
	
	public void setPreviousBlock(Block block) {
		
	}
	
	public String getHashtoString() {
		return hashCode.toString();
	}
}
