/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IIMFIsAlgorithm;

import java.util.List;

/**
 *
 * @author Hussein
 */
class IMFIs {
    
    int[] IMFIs;
    int Support;
    
    
    public IMFIs(int item){
		IMFIs = new int[]{item};
	}

   /**
	 * Constructor 
	 * @param items an array of items that should be added to the new itemset
	 */
	public IMFIs(int [] items){
		this.IMFIs = items;
	}
        /**
	 * Constructor 
	 * @param items a list of Integer representing items in the itemset
	 * @param support the support of the itemset
	 */
	public IMFIs(List<Integer> IMFIs, int support){
		this.IMFIs = new int[IMFIs.size()];
	    int i = 0;
	    for (int item : IMFIs) { 
	    	this.IMFIs[i++] = item;
	    }
	    this.Support = support;
	}
        /**
	 * Get the support of this itemset
	 */
	public int getAbsoluteSupport(){
		return Support;
	}
	
	/**
	 * Get the size of this itemset 
	 */
	public int size() {
		return IMFIs.length;
	}

	/**
	 * Get the item at a given position in this itemset
	 */
	public int get(int position) {
		return IMFIs[position];
	}

	/**
	 * Set the support of this itemset
	 * @param support the support
	 */
	public void setAbsoluteSupport(int support) {
		this.Support = support;
	}
    
}
