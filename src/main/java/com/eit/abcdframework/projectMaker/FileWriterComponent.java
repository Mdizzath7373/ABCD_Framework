package com.eit.abcdframework.projectMaker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileWriterComponent {

	public void writeController(String instanceForService, ArrayList<ArrayList<String>> list, String controllerPath) {
		ArrayList<String> lines = new ArrayList<String>();

		// Preparing arraylist to print in the file
		try (BufferedReader reader = new BufferedReader(new FileReader(controllerPath))) {

			String eachLine;
			while ((eachLine = reader.readLine()) != null) {
				lines.add(eachLine);
				if (eachLine.trim().replace(" ", "").contains("publicclass")) {
					lines.add(instanceForService);
					for (ArrayList<String> innerList : list) {
						lines.add("");
						lines.addAll(innerList);
					}
					lines.add("}");
					break;
				}
			}
//			for(String s : lines) {
//				System.out.println(s);
//			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writeOnFile(lines, controllerPath);
	}

	public void writeOnGridService(ArrayList<ArrayList<String>> list) {
		ArrayList<String> allLines = new ArrayList<String>();
		String gridServicePath = "F:\\New Git Clones\\demo\\src\\main\\java\\com\\example\\project\\services\\GridService.java";
		String javaFunctionFilePath = "C:\\Users\\Nadim\\Downloads\\javafunction.txt";

		String otherLogics = getOtherLogicsFromFile(javaFunctionFilePath, "JSONArraycolumnArray=newJSONArray();",new ArrayList<>());

		try (BufferedReader reader = new BufferedReader(new FileReader(gridServicePath))) {
			String eachLine;
			while ((eachLine = reader.readLine()) != null) {
				allLines.add(eachLine);
				if (eachLine.trim().replaceAll(" ", "")
						.equalsIgnoreCase("privatestaticfinalLoggerLOGGER=LoggerFactory.getLogger(\"GridService\");")) {
					for (int i = 0; i < list.size(); i++) {
						allLines.add("");
						allLines.addAll(list.get(i));
						allLines.add(otherLogics);
					}
					allLines.add("}");
					break;
				}

			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writeOnFile(allLines, gridServicePath);

	}

	public void writeOnWidgetService(ArrayList<ArrayList<String>> list) {
		String javaFunctionFilePath = "C:\\Users\\Nadim\\Downloads\\widgetfunction.txt";
		String widgetFunctionFilePath = "F:\\New Git Clones\\demo\\src\\main\\java\\com\\example\\project\\services\\WidgetService.java";
		ArrayList<String> allLines = new ArrayList<String>();

		String otherLogics = getOtherLogicsFromFile(javaFunctionFilePath, "JSONArraywidgetArray=newJSONArray();",new ArrayList<>());

		try (BufferedReader reader = new BufferedReader(new FileReader(widgetFunctionFilePath))) {
			String eachLine;
			while ((eachLine = reader.readLine()) != null) {
				allLines.add(eachLine);
				if (eachLine.trim().replaceAll(" ", "").equalsIgnoreCase(
						"privatestaticfinalLoggerLOGGER=LoggerFactory.getLogger(\"WidgetService\");")) {
					for (int i = 0; i < list.size(); i++) {
						allLines.addAll(list.get(i));
						allLines.add(otherLogics);
					}
					allLines.add("}");
					break;
				}

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writeOnFile(allLines, widgetFunctionFilePath);

	}

	public void writeOnFormsService(ArrayList<ArrayList<String>> list, ArrayList<ArrayList<String>> anotherLogic) {
		ArrayList<String> allLines = new ArrayList<String>();
		String formServicePath = "F:\\New Git Clones\\demo\\src\\main\\java\\com\\example\\project\\services\\FormService.java";

		String formPostFunctionFilePath = "C:\\Users\\Nadim\\Downloads\\formpostfunction.txt";
		String formPutFunctionFilePath = "C:\\Users\\Nadim\\Downloads\\formputfunction.txt";
		String formDeleteFunctionFilePath = "C:\\Users\\Nadim\\Downloads\\formdeletefunction.txt";
		String formGetFunctionFilePath = "C:\\Users\\Nadim\\Downloads\\formgetfunction.txt";

		String otherLogicsForPost = getOtherLogicsFromFile(formPostFunctionFilePath, "Stringresponse=\"\";",anotherLogic);
		String otherLogicsForPut = getOtherLogicsFromFile(formPutFunctionFilePath, "Stringresponse=\"\";",anotherLogic);
		String otherLogicsForDelete = getOtherLogicsFromFile(formDeleteFunctionFilePath,
				"JSONObjectreturndata=newJSONObject();",new ArrayList<>());
		String otherLogicsForGet = getOtherLogicsFromFile(formGetFunctionFilePath, "Stringurl=pgresturl;",new ArrayList<>());
		for (ArrayList<String> a : list) {
			for (int i = 0; i < a.size(); i++) {
				System.out.println("index " + i + a.get(i));
			}
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(formServicePath))) {
			String eachLine;
			while ((eachLine = reader.readLine()) != null) {
				allLines.add(eachLine);
				if (eachLine.trim().replaceAll(" ", "").equalsIgnoreCase("HttpclientcallerdataTransmits;")) {
					for (int i = 0; i < list.size(); i++) {
						ArrayList<String> innerList = list.get(i);
						allLines.addAll(list.get(i));

						if (innerList.get(0).contains("post")) {
								allLines.add(otherLogicsForPost);
						} else if (innerList.get(0).contains("put")) {
							allLines.add(otherLogicsForPut);
						} else if (innerList.get(0).contains("delete")) {
							allLines.add(otherLogicsForDelete);
						} else if (innerList.get(0).contains("get")) {
							allLines.add(otherLogicsForGet);
						}
					}
					allLines.add("}");
					break;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// for(String line : allLines) System.out.println(line);
		writeOnFile(allLines, formServicePath);

	}

	public void pageHtml(Map<String, List<String>> pageConfig) {

		try {
			String htmlPathSingletab = "F:\\pageHTML.txt";

			String cssPathSingletab = "F:\\pageCSS.txt";
			AtomicBoolean multiPage = new AtomicBoolean(false);
			String writePath = "F:\\refa_ad\\src\\app\\page\\";
			pageConfig.entrySet().stream().forEach(entry -> {
				multiPage.set(false);
				entry.getKey();

				if (entry.getValue().size() > 1) {
					multiPage.set(true);
				}
				String folderPath = writePath + entry.getKey().split("-")[1];

				entry.getValue().stream().forEach(tab -> {

					boolean loopDone = false;
					int c = 0;
					do {
						List<String> lines = new ArrayList<String>();
						String currentWritePath = "";
						String currentReadPath = "";

						if (!multiPage.get()) {
							c++;
							if (c == 1) {
								currentReadPath = htmlPathSingletab;
								currentWritePath = folderPath + "\\" + entry.getKey().split("-")[1] + ".component.html";
							} else if (c == 2) {
								currentReadPath = cssPathSingletab;
								currentWritePath = folderPath + "\\" + entry.getKey().split("-")[1] + ".component.scss";
							}
							try (BufferedReader reader = new BufferedReader(new FileReader(currentReadPath))) {
								String eachLine;
								while ((eachLine = reader.readLine()) != null) {
									lines.add(eachLine);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							File folder = new File(folderPath);

							System.err.println(folder.mkdir());

							try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentWritePath))) {
								for (String eachLine : lines) {
									writer.write(eachLine);
									writer.newLine();
									writer.flush();
								}

							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						if (currentWritePath.split("\\.")[2].equalsIgnoreCase("scss")) {
							loopDone = true;
						}
						System.err.println(loopDone);
					} while (!loopDone);

				});

			});

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void writeOnFile(ArrayList<String> lines, String filePath) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			for (int i = 0; i < lines.size(); i++) {
				writer.write(lines.get(i));
				writer.newLine();
			}
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getOtherLogicsFromFile(String FunctionFilePath, String startingLine, ArrayList<ArrayList<String>> anotherLogic) {
		String otherLogics = "";
		try (BufferedReader reader = new BufferedReader(new FileReader(FunctionFilePath))) {
			boolean isLineReached = false;
			String eachLine;
			while ((eachLine = reader.readLine()) != null) {
				if (eachLine.trim().replaceAll(" ", "").equalsIgnoreCase(startingLine)) {
					isLineReached = true;
				}
				if (isLineReached) {
					if (eachLine.trim().replaceAll(" ", "").equalsIgnoreCase(
							"response=dataTransmits.transmitDataspgrestpost(url,body,false);") &&!anotherLogic.isEmpty()) {
					List<String> list=anotherLogic.get(0);
						for (int j = 0; j < list.size(); j++) {
							otherLogics+=list.get(j) +"\n";
						}
					}
					otherLogics += eachLine + "\n";
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return otherLogics;
	}
}
