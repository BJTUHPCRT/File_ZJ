package org.cloudbus.cloudsim.examples.power.DRLVMC;

import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G4Xeon3040;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G5Xeon3075;

public class DRLVMCConstants {

    public final static String pythonCode = "../Deep-Learning/DRL.py";

    public final static String pythonClass = "DRL";

    public final static double meanGaussian = 12;

    public final static double stdGaussian = 5;

    public final static double vmTimemeanGaussian = 1800;

    public final static double vmTimestdGaussian = 300;

    public final static int NUMBER_OF_VMS = 50;

    public final static long CLOUDLET_UTILIZATION_SEED = 1;

    public final static int NUMBER_OF_HOSTS = 200;


    public final static boolean ENABLE_OUTPUT = false;
    public final static boolean OUTPUT_CSV    = false;

    public final static double SCHEDULING_INTERVAL = 300;
    public final static double SIMULATION_LIMIT = 24*60*60*2;

    public final static int CLOUDLET_LENGTH	=  5 * 60 * 1000;
    public final static int CLOUDLET_PES	= 1;

    public  final  static  int NUMBER_OF_DATA_SAMPLES = (int) Math.ceil( SIMULATION_LIMIT / SCHEDULING_INTERVAL)+1;
    public  final  static double cpuUpperUtilizationThreshold = 0.9;
    public  final  static double cpuLowerUtilizationThreshold = 0.2;

    public final static int VM_TYPES	= 4;
    public final static int[] VM_MIPS	= { 2500, 2000, 1000, 500 };
    public final static int[] VM_PES	= { 1, 1, 1, 1 };
    public final static int[] VM_RAM	= { 870,  1740, 1740, 613 };
    public final static int VM_BW		= 100000;
    public final static int VM_SIZE		= 2500;

    public final static int HOST_TYPES	 = 2;
    public final static int[] HOST_MIPS	 = { 1860, 2660 };
    public final static int[] HOST_PES	 = { 2, 2 };
    public final static int[] HOST_RAM	 = { 4096, 4096 };
    public final static int HOST_BW		 = 1000000;
    public final static int HOST_STORAGE = 1000000;

    public final static PowerModel[] HOST_POWER = {
            new PowerModelSpecPowerHpProLiantMl110G4Xeon3040(),
            new PowerModelSpecPowerHpProLiantMl110G5Xeon3075()
    };

}
