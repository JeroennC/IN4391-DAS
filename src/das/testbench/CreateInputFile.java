package das.testbench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

/**
 * Converts the game trace input into a list of commands
 *
 */
public class CreateInputFile {

	public static void main(String[] args) {
		List<String> input;
		List<String> output = new LinkedList<String>();;
		try {
			input = Files.readAllLines(Paths.get("C:/Users/jeroe/Documents/Programming/Distributed/GametraceInput"));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		int spawnInterval = 100;
		int currentHM = 0;
		int timeMSsince = 0;
		int clientId;
		int newHM;
		String mig;
		String[] content;
		
		for (String line : input ) {
			content = line.split("\t");
			clientId = Integer.parseInt(content[0]);
			newHM = Integer.parseInt(content[1]);
			if (newHM > currentHM) {
				// Sleep until 20min are up
				if (timeMSsince < 20000) {
					output.add("sleep " + (20000 - timeMSsince) + "\n");
				}
				timeMSsince = 0;
				currentHM = newHM;
			}
			mig = content[2];
			if (mig.equals("Come")) {
				output.add("start Client " + clientId + "\n");
			} else {
				output.add("kill Client " + clientId + "\n");
			}
			// Sleep
			output.add("sleep " + spawnInterval + "\n");
			timeMSsince += spawnInterval;
		}
		
		Path pOutput = Paths.get("C:/Users/jeroe/Documents/Programming/Distributed/GametraceCommands");
		for (String line : output) {
			try {
				Files.write(pOutput, line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
