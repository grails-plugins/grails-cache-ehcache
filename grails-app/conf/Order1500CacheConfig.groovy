order = 1500

config = {
	cache {
		name 'the_cache'
		eternal false
		overflowToDisk true
		maxBytesLocalHeap "20M"
		maxElementsOnDisk 3
	}
}
