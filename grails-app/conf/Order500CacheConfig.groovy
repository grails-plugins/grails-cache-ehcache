order = 500

config = {
	cache {
		name 'the_cache'
		eternal false
		overflowToDisk true
		maxElementsInMemory 10000
		maxElementsOnDisk 10000000
	}
}
