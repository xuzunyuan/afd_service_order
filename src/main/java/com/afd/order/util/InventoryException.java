package com.afd.order.util;

public class InventoryException extends Exception {
	public InventoryException(){
		super();
	}
	
	public InventoryException(String msg){
		super(msg);
	}
	
	public InventoryException(String msg,Throwable cause){
		super(msg,cause);
	}
}
