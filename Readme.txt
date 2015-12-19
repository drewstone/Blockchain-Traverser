Team: Drew Stone, Liam Gallagher

We created a bitcoin transaction graph. We downloaded a local copy of the block chain, but 
found parsing transactions from this to be extremely difficult, even with the library bitcoinj.
Instead, we used jSoup to pull parsed transaction data from the website blockexplorer.com, and
constructed a graph of transactions from this. We analyzed the size and number of weakly and
strongly connected components, and the results of applying PageRank to the graph.  

This was an empirical analysis project. Our code is rough, and is the unpolished and inefficient
but successful result of attempts to find strongly and weakly connected components, page rank
values, and print the results. This was a construction of a graph, and application of graph
algorithms. We used DFS to find weakly connected components, and applied Kosaraju’s algorithm
to find the elusive strongly connected components. We were able to test the results of our code
by using the website blockexplorer.com and blockchain.info to confirm that we were receiving 
accurate information.     

