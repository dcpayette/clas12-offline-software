//Author: David Payette and Nate Dzbenski

/* This code takes the time-reduced tracks produced by the Time Average, as well as the original hits 
 * and uses a fit formula from garfield++ to calculate the hit's position in the drift region
 * based on the time of the signal. We use the original hits to see how well the formula performs 
 * when we include factors such as time shifts and non-uniform magnetic fields
 */

package org.jlab.rec.rtpc.hit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JFrame;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.graphics.EmbeddedCanvas;

public class TrackHitReco {
    
    //MAGBOLTZ PARAMETERS DO NOT TOUCH
    /*final private double a_t1 = -2.48491E-4;
    final private double a_t2 = 2.21413E-4;
    final private double a_t3 = -3.11195E-3;
    final private double a_t4 = -2.75206E-1;
    final private double a_t5 = 1.74281E3;
    private double a_t;

    final private double b_t1 = 2.48873E-5;
    final private double b_t2 = -1.19976E-4;
    final private double b_t3 = -3.75962E-3;
    final private double b_t4 = 5.33100E-2;
    final private double b_t5 = -1.25647E2;
    private double b_t;
    
    final private double a_phi1 = -3.32718E-8;
    final private double a_phi2 = 1.92110E-7;
    final private double a_phi3 = 2.16919E-6;
    final private double a_phi4 = -8.10207E-5;
    final private double a_phi5 = 1.68481E-1;
    private double a_phi;
    
    final private double b_phi1 = -3.23019E-9;
    final private double b_phi2 = -6.92075E-8;
    final private double b_phi3 = 1.24731E-5;
    final private double b_phi4 = 2.57684E-5;
    final private double b_phi5 = 2.10680E-2;
    private double b_phi;*/
    private double a_t;
    private double b_t;
    private double a_phi;
    private double b_phi;
    
    //COSMIC MAGBOLTZ PARAMETERS
    final private double a_t1 = 0;//2.29627e-05;
    final private double a_t2 = 0;//-3.93146e-05;
    final private double a_t3 = 0;//-5.28600e-03;
    final private double a_t4 = 0;//2.78240e-02;
    final private double a_t5 = 6.96387e+02;//6.96145e+02;
    
    final private double b_t1 = 0;//-7.35833e-06;
    final private double b_t2 = 0;//6.81331e-05;
    final private double b_t3 = 0;//1.53858e-03;
    final private double b_t4 = 0;//-2.31373e-02;
    final private double b_t5 = -4.73759e+01;//-4.74214e+01;
    
    final private double a_phi1 = 0;//1.12871e-08;
    final private double a_phi2 = 0;//1.50882e-08;
    final private double a_phi3 = 0;//-3.83492e-06;
    final private double a_phi4 = 0;//-3.43608e-06;
    final private double a_phi5 = 0;//2.16630e-05;
    
    final private double b_phi1 = 0;//-5.03125e-09;
    final private double b_phi2 = 0;//-4.94344e-09;
    final private double b_phi3 = 0;//1.77409e-06;
    final private double b_phi4 = 0;//2.73528e-06;
    final private double b_phi5 = 0;//-3.45254e-05;
    
    final private double t_2GEM2 = 296.082;
    final private double t_2GEM3 = 296.131;
    final private double t_2PAD = 399.09;
    final private double t_gap = 500;//t_2GEM2 + t_2GEM3 + t_2PAD;

    final private double phi_2GEM2 = 0.0492538;
    final private double phi_2GEM3 = 0.0470817;
    final private double phi_2PAD = 0.0612122;
    final private double phi_gap = 0;//phi_2GEM2 + phi_2GEM3 + phi_2PAD;
    
    
    private double larget;
    private double smallt;
    private double tdiff;
    private double Time;
    private int cellID;

    private double drifttime;
    private double r_rec;
    private double dphi;
    private double phi_rec;
    private double x_rec;
    private double y_rec;
   
    private boolean cosmic = false;
    
    public TrackHitReco(HitParameters params, List<Hit> rawHits, boolean draw) {

        HashMap<Integer, Double> tdiffmap = new HashMap<>();
        HashMap<Integer, List<RecoHitVector>> recotrackmap = new HashMap<>();
        ReducedTrackMap RTIDMap = params.get_rtrackmap();
        List<Integer> tids = RTIDMap.getAllTrackIDs();
        /*GraphErrors gxy = new GraphErrors();
        GraphErrors grz = new GraphErrors();
        EmbeddedCanvas c = new EmbeddedCanvas();
        c.divide(1,2);
        JFrame j = new JFrame();
        j.setSize(800,800);
        
        
        
        try {

            File out = new File("/Users/davidpayette/Desktop/SignalStudies/");
            if(!out.exists())
            {out.mkdirs();}
            //FileWriter write = new FileWriter("/Users/davidpayette/Desktop/SignalStudies/trackenergy.txt",true); 
            FileWriter write = new FileWriter("/Users/davidpayette/Desktop/SignalStudies/timespectra.txt",true); */
        for(int TID : tids) {
            double adc = 0;
            ReducedTrack track = RTIDMap.getTrack(TID);
            //System.out.println(track.getAllHits().size() + " number of hits");
            track.sortHits();
            smallt = track.getSmallT();
            larget = track.getLargeT();
            //write.write(smallt + "\t" + larget + "\r\n");
            
            //tdiff = 6000 - larget;
            if(cosmic) tdiff = 2025 - larget;
            else tdiff = 6000 - larget;
            //tdiff = 1800-larget;
            //tdiff = 0;
            tdiffmap.put(TID, tdiff);
            recotrackmap.put(TID, new ArrayList<>());
            List<HitVector> allhits = track.getAllHits();
            
            for(HitVector hit : allhits) {
                adc += hit.adc();
                cellID = hit.pad();              
                Time = hit.time();
                //System.out.println("Track Reco " + Time);
                Time += tdiff;
		           
                // find reconstructed position of ionization from Time info		                
                drifttime = Time-t_gap;
                r_rec = get_r_rec(hit.z(),drifttime); //in mm
                dphi = get_dphi(hit.z(),r_rec); // in rad
                
                phi_rec=hit.phi()-dphi-phi_gap;
                
                if(phi_rec<0.0) {
                    phi_rec+=2.0*Math.PI;
                }
                if(phi_rec>2.0*Math.PI){
                    phi_rec-=2.0*Math.PI;
                }

                // x,y,z pos of reconstructed track
                x_rec=r_rec*(Math.cos(phi_rec));
                y_rec=r_rec*(Math.sin(phi_rec));
                //gxy.addPoint(x_rec, y_rec, 0, 0);
                //System.out.println("reco" + x_rec + " " + y_rec);
                //grz.addPoint(r_rec, hit.z(),0,0);
                recotrackmap.get(TID).add(new RecoHitVector(cellID,x_rec,y_rec,hit.z(),tdiff,Time));
            }
            //write.write(adc + "\r\n");
        }
        /*GraphErrors gsimxy = new GraphErrors();
        gsimxy.setMarkerColor(2);
        gsimxy.setMarkerSize(1);
        gxy.setMarkerSize(2);
        
        if(draw){for(Hit hit : rawHits){
            gsimxy.addPoint(hit.get_PosXTrue(), hit.get_PosYTrue(), 0, 0);
        }
        GraphErrors gcircles = new GraphErrors();
        gcircles.setMarkerSize(0);
        double theta = 0;
        double test_x = 0;
        double test_y = 0;
        double test_x2 = 0;
        double test_y2 = 0;
        while(theta <= 2*Math.PI)
        {
                test_x = 30 * Math.cos(theta);
                test_y = 30 * Math.sin(theta);
                test_x2 = 70 * Math.cos(theta);
                test_y2 = 70 * Math.sin(theta);
                gcircles.addPoint(test_x, test_y, 0, 0);
                gcircles.addPoint(test_x2, test_y2, 0, 0);
                theta+=0.01;
        }
        c.cd(0);
        c.draw(gxy);
        c.draw(gcircles,"same");
        c.draw(gsimxy,"same");
        //c.cd(1);
        //c.draw(grz);
        j.add(c);
        j.setVisible(true);}
        
                    write.close();
            } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }*/
        params.set_recotrackmap(recotrackmap);
    }	
	
    private double get_rec_coef(double t1, double t2, double t3, double t4, double t5, double z2) {
        double z = 0;//z2/1000;
        return t1*z*z*z*z + t2*z*z*z + t3*z*z + t4*z + t5;
    }

    private double get_r_rec(double z,double t){
        a_t = get_rec_coef(a_t1,a_t2,a_t3,a_t4,a_t5,z);
        b_t = get_rec_coef(b_t1,b_t2,b_t3,b_t4,b_t5,z);
	return ((-(Math.sqrt(a_t*a_t+(4*b_t*t)))+a_t+(14*b_t))/(2*b_t))*10.0;
    }
    
    private double get_dphi(double z, double r){
        a_phi = get_rec_coef(a_phi1,a_phi2,a_phi3,a_phi4,a_phi5,z);
        b_phi = get_rec_coef(b_phi1,b_phi2,b_phi3,b_phi4,b_phi5,z);
        return a_phi*(7-r/10)+b_phi*(7-r/10)*(7-r/10); // in rad
    }
    
    
}




