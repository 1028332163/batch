package neu.lab.batch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.commons.exec.ExecuteException;


public class BatchScript {

	static Map<String, String> module2conflict = new java.util.LinkedHashMap<String, String>();
	static String baseDir = "D:\\ws_testcase\\reportProject\\";
	static String mvnPath = "D:\\cTool\\apache-maven-3.2.5\\bin\\mvn.bat";
	static String geneGoal = "neu.lab:riddle:1.0:gene";
	static String tmpWsDir = "D:\\ws_testcase\\tempWs\\";
	static String singleTracePath = tmpWsDir + "TraceLog.txt";
	
	static String allTracePath = "D:\\ws_testcase\\allErrorTrace.txt";
	static String allPathDirPath = "D:\\ws_testcase\\allPath\\";
	
	static PrintWriter allTracePrinter;

	static {
		try {
			allTracePrinter = new PrintWriter(new BufferedWriter(new FileWriter(allTracePath)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		FileUtil.delFolder(allPathDirPath, true);
		new File(allPathDirPath).mkdirs();
		try {
			//			"D:\\cWS\\notepad++\\reportList_origin.txt"
			//			"reportList.txt"
			System.out.println(new File("reportList_after.txt").getAbsolutePath());
			BufferedReader reader = new BufferedReader(new FileReader("reportList_after.txt"));
			String line = reader.readLine();
			String module = null;
			while (line != null) {
				if (!line.equals("")) {
					if (line.startsWith("module:"))
						module = extractModule(line);
					if (line.startsWith("conflict:")) {
						String conflict = extractConflict(line);
						module2conflict.put(module, conflict);
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		exeAllProject();
		allTracePrinter.close();
	}

	private static String getCmd(String module, String conflict) {
		String cmd = mvnPath + " -f=" + baseDir + module + " -DcallConflict=\"" + conflict
				+ "\" -Dmaven.test.skip=true package " + geneGoal + " -e";
		return cmd;
	}

	private static void exeAllProject() {
		int doneCnt = 0;

		for (String module : module2conflict.keySet()) {
			System.out.println(getCmd(module, module2conflict.get(module)));
			
			allTracePrinter.println("++++ result for " + baseDir + module);
			allTracePrinter.println("conflict: " + baseDir + module);
			
			try {
				ExecUtil.exeCmd(getCmd(module, module2conflict.get(module)));
			} catch (ExecuteException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (checkErrorTraceFile()) {
				allTracePrinter.println("success!");
				moveSingleToAll();
			} else {
				allTracePrinter.println("fail!");
			}
			allTracePrinter.flush();
			movePathFile();	
			allTracePrinter.println();
			allTracePrinter.println();
			allTracePrinter.println();
			doneCnt++;
			System.out.println("doneCnt/all:" + doneCnt + "/" + module2conflict.keySet().size());
		}
		

	}

	private static void movePathFile() {
		File tmpWs = new File(tmpWsDir);
		for (File child : tmpWs.listFiles()) {
			if (child.getName().startsWith("d_") || child.getName().startsWith("p_")) {
				FileUtil.copyFile(child.getAbsolutePath(), allPathDirPath+child.getName());
			}
		}

	}

	private static void moveSingleToAll() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(singleTracePath));
			String line = reader.readLine();
			while (line != null) {
				allTracePrinter.println(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void moveDisAndPath() {

	}

	private static String extractModule(String line) {
		return line.replace("module:", "");
	}

	private static String extractConflict(String line) {
		return line.replace("conflict:", "");
	}

	private static boolean checkErrorTraceFile() {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(singleTracePath));
		} catch (FileNotFoundException e) {
			System.out.println("can't find trace file.");
			return false;
		}
		try {
			String line = reader.readLine();
			while (line != null) {
				if (line.contains("java.lang.NoClassDefFoundError") || line.contains("java.lang.ClassNotFoundException")
						|| line.contains("java.lang.NoSuchMethodError")
						|| line.contains("java.lang.NoSuchMethodException")
						|| line.contains("java.lang.AbstractMethodError")) {
					return true;
				}
				line = reader.readLine();
			}
			return false;
		} catch (Exception e) {
			System.out.println("confuse exception.");
			return false;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
