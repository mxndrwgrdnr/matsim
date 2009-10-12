package playground.wrashid.lib;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioLoader;

import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;

public class GeneralLib {

	/*
	 * Reads the population from the plans file.
	 * 
	 * Note: use the other method with the same name, if this poses problems.
	 */
	public static Population readPopulation(String plansFile, String networkFile) {
		Population population = new PopulationImpl();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);

		PopulationReader popReader = new MatsimPopulationReader(population, network);
		popReader.readFile(plansFile);

		return population;
	}

	/*
	 * Reads the population from the plans file.
	 */
	public static Population readPopulation(String plansFile, String networkFile, String facilititiesPath) {
		Scenario sc = new ScenarioImpl();

		sc.getConfig().setParam("plans", "inputPlansFile", plansFile);
		sc.getConfig().setParam("network", "inputNetworkFile", networkFile);
		sc.getConfig().setParam("facilities", "inputFacilitiesFile", facilititiesPath);

		ScenarioLoader sl = new ScenarioLoader((ScenarioImpl) sc);

		sl.loadScenario();

		return sc.getPopulation();
	}

	/*
	 * Write the population to the specified file.
	 */
	public static void writePopulation(Population population, String plansFile) {
		MatsimWriter populationWriter = new PopulationWriter(population);

		populationWriter.write(plansFile);
	}

	public static ActivityFacilitiesImpl readActivityFacilities(String facilitiesFile) {
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(facilitiesFile);
		return facilities;
	}

	/*
	 * Write the facilities to the specified file.
	 */
	public static void writeActivityFacilities(ActivityFacilitiesImpl facilities, String facilitiesFile) {
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(facilities, facilitiesFile);
		facilitiesWriter.write();
	}

	/**
	 * Write out a list of Strings
	 * 
	 * after each String in the list a "\n" is added.
	 * 
	 * @param list
	 * @param fileName
	 */
	public static void writeList(ArrayList<String> list, String fileName) {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fos);

			for (int i = 0; i < list.size(); i++) {
				outputStreamWriter.write(list.get(i));
				outputStreamWriter.write("\n");
			}

			outputStreamWriter.flush();
			outputStreamWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * if headerLine=null, then add no line at top of file. "\n" is added at end of first line by this method.
	 * 
	 * matrix[numberOfRows][numberOfColumns]
	 * 
	 * @param matrix
	 * @param fileName
	 * @param headerLine
	 */
	public static void writeMatrix(double[][] matrix, String fileName, String headerLine){
		ArrayList<String> list=new ArrayList<String>();
		
		if (headerLine!=null){
			list.add(headerLine);
		}
		
		for (int i=0;i<matrix.length;i++){
			String line="";
			for (int j=0;j<matrix[0].length-1;j++){
				line+=matrix[i][j];
				line+="\t";
			}
			line+=matrix[i][matrix[0].length-1];
			list.add(line);
		}
		
		writeList(list, fileName);
	}

	/**
	 * reads in data from a file.
	 * 
	 * 
	 * @param numberOfRows
	 * @param numberOfColumns
	 * @param ignoreFirstLine
	 * @return
	 */
	public static double[][] readMatrix(int numberOfRows, int numberOfColumns, boolean ignoreFirstLine, String fileName) {

		double[][] matrix = new double[numberOfRows][numberOfColumns];

		try {

			FileReader fr = new FileReader(fileName);

			BufferedReader br = new BufferedReader(fr);
			String line;
			StringTokenizer tokenizer;
			String token;

			if (ignoreFirstLine) {
				br.readLine();
			}

			line = br.readLine();
			int rowId = 0;
			while (line != null) {
				tokenizer = new StringTokenizer(line);

				for (int i = 0; i < numberOfColumns; i++) {
					token = tokenizer.nextToken();
					double parsedNumber = Double.parseDouble(token);
					matrix[rowId][i] = parsedNumber;
				}

				if (tokenizer.hasMoreTokens()) {
					// if there are more columns than expected, throw an
					// exception
					throw new RuntimeException("the number of columns is wrong");
				}

				line = br.readLine();
				rowId++;
			}
			if (rowId != numberOfRows) {
				throw new RuntimeException("the number of rows is wrong");
			}

		} catch (RuntimeException e) {
			// just forward the runtime exception
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Error reading the matrix from the file");
		}

		return matrix;
	}

	public static double[][] invertMatrix(double[][] matrix) {
		int firstDimentionOfResultMatrix = matrix[0].length;
		int secondDimentionOfResultMatrix = matrix.length;

		double[][] resultMatrix = new double[firstDimentionOfResultMatrix][secondDimentionOfResultMatrix];

		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				resultMatrix[j][i] = matrix[i][j];
			}
		}

		return resultMatrix;
	}

	/**
	 * TODO This method
	 * 
	 * @param fileName
	 * @return
	 */
	public static String getFirstLineOfFile(String fileName) {
		return null;
	}
	
	
	// TODO: write tests.
	// The target 
	public static void copyDirectory(String sourceDirectoryPath, String targetDirectoryPath){
		
	}

}
