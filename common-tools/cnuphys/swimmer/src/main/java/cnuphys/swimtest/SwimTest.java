package cnuphys.swimtest;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import cnuphys.lund.LundStyle;
import cnuphys.lund.LundTrackDialog;
import cnuphys.lund.SwimTrajectoryListener;
import cnuphys.magfield.FastMath;
import cnuphys.magfield.MagneticFieldCanvas;
import cnuphys.magfield.MagneticFieldChangeListener;
import cnuphys.magfield.MagneticFieldInitializationException;
import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.MagneticFields.FieldType;
import cnuphys.rk4.RungeKuttaException;
import cnuphys.swim.SwimMenu;
import cnuphys.swim.SwimTrajectory;
import cnuphys.swim.Swimmer;
import cnuphys.swim.Swimming;
import cnuphys.swimZ.SwimZStateVector;

public class SwimTest {
	
	//we only need one swimmer it will adapt to changing fields.
	private static Swimmer _swimmer;
	
	private static int _sector = 1;
	final static MagneticFieldCanvas canvas1 = new MagneticFieldCanvas(_sector, -50, 0, 650, 350.,
			MagneticFieldCanvas.CSType.XZ);
//	final static MagneticFieldCanvas canvas2 = new MagneticFieldCanvas(_sector, -50, 0, 650, 350.,
//			MagneticFieldCanvas.CSType.YZ);

	private static final JMenuItem reconfigItem = new JMenuItem("Remove Solenoid and Torus Overlap");

	private static double[] adaptiveAbsError = { 1.0e-5, 1.0e-5, 1.0e-5, 1.0e-5 };

	private static double oldAccuracy = 1.0e-6; // z stopping accuracy in m
	private static double oldAdaptiveInitStepSize = 0.01; // m

	//initialize the magnetic field
	private static void initMagField() {
		// test specific load
		final MagneticFields mf = MagneticFields.getInstance();
		File mfdir = new File(System.getProperty("user.home"), "magfield");
		System.out.println("mfdir exists: " + (mfdir.exists() && mfdir.isDirectory()));
		try {
			mf.initializeMagneticFields(mfdir.getPath(), "Full_torus_r251_phi181_z251_08May2018.dat",
					"Symm_solenoid_r601_phi1_z1201_2008.dat");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (MagneticFieldInitializationException e) {
			e.printStackTrace();
			System.exit(1);
		}

		MagneticFields.getInstance().setActiveField(FieldType.TORUS);

	}

	/**
	 * Print a memory report
	 * 
	 * @param message
	 *            a message to add on
	 */
	public static void memoryReport(String message) {
		System.gc();
		System.gc();

		StringBuilder sb = new StringBuilder(1024);
		double total = (Runtime.getRuntime().totalMemory()) / 1048576.;
		double free = Runtime.getRuntime().freeMemory() / 1048576.;
		double used = total - free;
		sb.append("==== Memory Report =====\n");
		if (message != null) {
			sb.append(message + "\n");
		}
		sb.append("Total memory in JVM: " + String.format("%6.1f", total) + "MB\n");
		sb.append(" Free memory in JVM: " + String.format("%6.1f", free) + "MB\n");
		sb.append(" Used memory in JVM: " + String.format("%6.1f", used) + "MB\n");

		System.err.println(sb.toString());
	}
	
	
	//create the test menu
	private static JMenu getTestMenu() {
		JMenu menu = new JMenu("Tests");
		
		final JMenuItem createTrajItem = new JMenuItem("Create Test Trajectories...");
		final JMenuItem testSectorItem = new JMenuItem("Test Sector Swim");
		final JMenuItem threadItem = new JMenuItem("Thread Test");
		final JMenuItem oneVtwoItem = new JMenuItem("Swimmer vs. Swimmer2 Test");

		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == createTrajItem) {
					CreateTestTrajectories.createTestTraj(3344632211L, 1000);
				}
				else if (e.getSource() == testSectorItem) {
					SectorTest.testSectorSwim();
				}
				else if (e.getSource() == threadItem) {
					ThreadTest.threadTest(100, 8);
				}
				else if (e.getSource() == oneVtwoItem) {
					CompareSwimmers.swimmerVswimmer2Test(3344632211L, 5000);
				}
				else if (e.getSource() == reconfigItem) {
					MagneticFields.getInstance().removeMapOverlap();
				}
				
			}
			
		};
		
		threadItem.addActionListener(al);	
		createTrajItem.addActionListener(al);	
		oneVtwoItem.addActionListener(al);	
		testSectorItem.addActionListener(al);	
		reconfigItem.addActionListener(al);	
		menu.add(createTrajItem);
		menu.add(oneVtwoItem);
		menu.add(testSectorItem);
		menu.add(reconfigItem);
		menu.add(threadItem);
		
		return menu;
	}
	
	private static JFrame createFrame() {
		final JFrame testFrame = new JFrame("Swim Test Frame");
		testFrame.setLayout(new BorderLayout(4, 4));
		

		final MagneticFields mf = MagneticFields.getInstance();

		
		final JLabel label = new JLabel(" Torus: " + MagneticFields.getInstance().getTorusPath());
		label.setFont(new Font("SandSerif", Font.PLAIN, 10));
		testFrame.add(label, BorderLayout.SOUTH);


		// drawing canvas
		JPanel magPanel1 = canvas1.getPanelWithStatus(1000, 465);
//		JPanel magPanel2 = canvas2.getPanelWithStatus(680, 460);
		canvas1.setExtraText("Field Magnitude (T)");
//		canvas2.setExtraText("Gradient Magnitude (T/m)");
//		canvas2.setShowGradient(true);

		// set up what to do if the window is closed
		WindowAdapter windowAdapter = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				System.err.println("Done");
				System.exit(1);
			}
		};

		MagneticFieldChangeListener mfcl = new MagneticFieldChangeListener() {

			@Override
			public void magneticFieldChanged() {
				label.setText(" Torus: " + MagneticFields.getInstance().getTorusPath());
				System.err.println("Field changed. Torus path: " + MagneticFields.getInstance().getTorusPath());
//				Swimming.clearAllTrajectories();
//				canvas1.clearTrajectories();
//				Swimming.clearAllTrajectories();
//				canvas2.clearTrajectories();
			}
			
		};
		MagneticFields.getInstance().addMagneticFieldChangeListener(mfcl);
		
		
		// add the menu
		JMenuBar mb = new JMenuBar();
		testFrame.setJMenuBar(mb);
		mb.add(mf.getMagneticFieldMenu());
		mb.add(SwimMenu.getInstance());
		
		JMenu testMenu = getTestMenu();

		mb.add(testMenu);
		
		JPanel cpanel = new JPanel();
		cpanel.setLayout(new GridLayout(1, 1, 4, 4));
				
		cpanel.add(magPanel1);
//		cpanel.add(magPanel2);
		
		testFrame.add(cpanel, BorderLayout.CENTER);

		testFrame.addWindowListener(windowAdapter);
		testFrame.pack();
		
		return testFrame;

	}
	
	//takes all the created SwimTrajectories
	private static void setMCanvasTrajectories() {
		canvas1.clearTrajectories();
//		canvas2.clearTrajectories();
		ArrayList<SwimTrajectory> trajectories = Swimming.getMCTrajectories();
		
		if ((trajectories == null) || (trajectories.size() < 1)) {
			return;
		}
		
		for (SwimTrajectory traj : trajectories) {
			double x[] = traj.getX();
			double y[] = traj.getY();
			double z[] = traj.getZ();
			
			LundStyle ls = LundStyle.getStyle(traj.getLundId());
			canvas1.addTrajectory(x, y, z, ls.getLineColor(), ls.getStroke());
//			canvas2.addTrajectory(x, y, z, ls.getLineColor(), ls.getStroke());
		}
	}


	private static void header(String s) {
		System.out.println("-------------------------------------");
		System.out.println("**  " + s);
		System.out.println("-------------------------------------");
	}

	private static void footer(String s) {
		System.out.print("---------[end ");
		System.out.print(s);
		System.out.println(" ]-------------\n");
	}


	/**
	 * Swims a charged particle. This swims to a fixed z value. This is for the
	 * trajectory mode, where you want to cache steps along the path. Uses an
	 * adaptive stepsize algorithm.
	 * 
	 * @param charge
	 *            the charge: -1 for electron, 1 for proton, etc
	 * @param xo
	 *            the x vertex position in meters
	 * @param yo
	 *            the y vertex position in meters
	 * @param zo
	 *            the z vertex position in meters
	 * @param momentum
	 *            initial momentum in GeV/c
	 * @param theta
	 *            initial polar angle in degrees
	 * @param phi
	 *            initial azimuthal angle in degrees
	 * @param fixedZ
	 *            the fixed z value (meters) that terminates (or maxPathLength
	 *            if reached first)
	 * @param accuracy
	 *            the accuracy of the fixed z termination, in meters
	 * @param sMax
	 *            Max path length in meters. This determines the max number of steps based on
	 *            the step size. If a stopper is used, the integration might
	 *            terminate before all the steps are taken. A reasonable value
	 *            for CLAS is 8. meters
	 * @param stepSize
	 *            the initial step size in meters.
	 * @param relTolerance
	 *            the error tolerance as fractional diffs. Note it is a vector,
	 *            the same dimension of the problem, e.g., 6 for
	 *            [x,y,z,vx,vy,vz]. It might be something like {1.0e-10,
	 *            1.0e-10, 1.0e-10, 1.0e-8, 1.0e-8, 1.0e-8}
	 * @param hdata
	 *            if not null, should be double[3]. Upon return, hdata[0] is the
	 *            min stepsize used (m), hdata[1] is the average stepsize used
	 *            (m), and hdata[2] is the max stepsize (m) used
	 * @return the trajectory of the particle
	 * @throws RungeKuttaException
	 */
	private SwimTrajectory traditionalSwimmer(int charge, double momentum, double theta, double phi, double ztarget,
			double accuracy, double maxPathLen, double stepSize, double[] tolerance, double[] hdata)
			throws RungeKuttaException {
		SwimTrajectory traj;
		traj = _swimmer.swim(charge, 0, 0, 0, momentum, theta, phi, ztarget, accuracy, maxPathLen, stepSize,
				Swimmer.CLAS_Tolerance, hdata);
		double finalStateVector[] = traj.lastElement();
		printSummary("\nresult from adaptive stepsize method with storage and Z cutoff at " + ztarget, traj.size(),
				momentum, finalStateVector, hdata);

		return traj;
	}
	
	/**
	 * Print a vector to the standard output
	 * @param v the double vector
	 * @param s an info string
	 */
	public static void printSwimZ(SwimZStateVector v, String s) {
				
		String out = String.format("%s [%-12.5f, %-12.5f, %-12.5f]", s, v.x/100., v.y/100., v.z/100.);
		System.out.println(out);
	}


	/**
	 * Print a vector to the standard output
	 * @param v the double vector
	 * @param s an info string
	 */
	public static void printVect(double v[], String s) {
		
		if (v.length == 8) {
			String out = String.format("%s [%-12.5f, %-12.5f, %-12.5f, %-12.5f, %-12.5f, %-12.5f, %-12.5f, %-12.5f]", 
					s, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]);
			System.out.println(out);
			return;
		}
		
		String out = String.format("%s [%-12.5f, %-12.5f, %-12.5f]", s, v[0], v[1], v[2]);
		System.out.println(out);
	}

	private static void printSummary(String message, int nstep, double momentum, double Q[], double hdata[]) {
		System.out.println(message);
		double R = Math.sqrt(Q[0] * Q[0] + Q[1] * Q[1] + Q[2] * Q[2]);
		double norm = Math.sqrt(Q[3] * Q[3] + Q[4] * Q[4] + Q[5] * Q[5]);
		double P = momentum * norm;

		System.out.println("Number of steps: " + nstep);

		if (hdata != null) {
			System.out.println("min stepsize: " + hdata[0]);
			System.out.println("avg stepsize: " + hdata[1]);
			System.out.println("max stepsize: " + hdata[2]);
		}
		System.out.println(
				String.format("R = [%8.5f, %8.5f, %8.5f] |R| = %7.4f cm\nP = [%7.4e, %7.4e, %7.4e] |P| =  %9.6e GeV/c",
						100 * Q[0], 100 * Q[1], 100 * Q[2], 100 * R, P * Q[3], P * Q[4], P * Q[5], P));
		System.out.println("norm (should be 1): " + norm);
		if (hdata != null) {
			hdataReport(hdata, 100.);
		}
		System.out.println("--------------------------------------\n");
	}

	private static void hdataReport(double hdata[], double toCM) {
		String s = String.format("Min step: %-12.5e   Avg Step: %-12.5f   Max Step: %-12.5f cm", toCM * hdata[0],
				toCM * hdata[1], toCM * hdata[2]);
		System.out.println(s);
	}
	

	/**
	 * main program
	 * @param arg command line arguments (ignored)
	 */
	public static void main(String arg[]) {

		initMagField();

		System.out.println("Active Field Description: " + MagneticFields.getInstance().getActiveFieldDescription());

		FastMath.setMathLib(FastMath.MathLib.SUPERFAST);
		// MagneticField.setMathLib(MagneticField.MathLib.DEFAULT);
		int numTest = 10000;
		
		JFrame testFrame = createFrame();
		
		SwimTrajectoryListener trajListener = new SwimTrajectoryListener() {

			@Override
			public void trajectoriesChanged() {
				ArrayList<SwimTrajectory> trajectories = Swimming.getMCTrajectories();
				System.out.println("Now have " + trajectories.size() +  " trajectories");
				setMCanvasTrajectories();
			}
			
		};
		
		
		Swimming.addSwimTrajectoryListener(trajListener);
		
		LundTrackDialog.getInstance().setFixedZSelected(true);
		
		_swimmer = new Swimmer();
		
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				testFrame.setVisible(true);
			}
		});		

	}
}
