order = 500

config = {
	cache {
		name 'the_cache'
		eternal false
		overflowToDisk true
		maxBytesLocalHeap "50M"
		maxElementsOnDisk 10000000
	}
	defaults {
		maxBytesLocalHeap "10M"
	}
}
