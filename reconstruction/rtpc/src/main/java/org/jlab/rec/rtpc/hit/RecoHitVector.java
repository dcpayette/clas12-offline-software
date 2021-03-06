package org.jlab.rec.rtpc.hit;

public class RecoHitVector {
	
	private int pad;
	private double x;
	private double y; 
	private double z;
	private double dt;
	private double time;
	
	public RecoHitVector()
	{
		pad = 0; 
		x = 0; 
		y = 0; 
		z = 0; 
		dt = 0;
		time = 0;
	}
	
	public RecoHitVector(int padnum, double xrec, double yrec, double zrec, double tdiff, double t)
	{
		pad = padnum; 
		x = xrec;
		y = yrec;
		z = zrec;
		dt = tdiff;
		time = t;
	}
	
	public void setpad(int padnum)
	{
		pad = padnum; 
	}
	
	public void setx(double xrec)
	{
		x = xrec;
	}
	
	public void sety(double yrec)
	{
		y = yrec;
	}
	
	public void setz(double zrec)
	{
		z = zrec;
	}
	
	public void settime(double t)
	{
		time = t; 
	}
	
	public void setdt(double tdiff)
	{
		dt = tdiff;
	}
	
	public int pad()
	{
		return pad;
	}
	
	public double x()
	{
		return x;
	}
	
	public double y()
	{
		return y;
	}
	
	public double z()
	{
		return z;
	}
	
	public double time()
	{
		return time;
	}
	
	public double dt()
	{
		return dt;
	}
		
	

}
