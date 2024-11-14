package com.eit.abcdframework.projectMaker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
@Component
public class FileWriterComponent {
	
		
	public void writeOnGridController(String instanceForService,List<Map<String,String>> list) {
		final String gridControllerPath ="F:\\New Git Clones\\ABCD_project_maker\\src\\main\\java\\com\\eit\\abcd\\projectmaker\\controller\\GridController.java";
		List<String> lines = new ArrayList<String>();
		
		//Preparing arraylist to print in the file
		try(BufferedReader reader = new BufferedReader(new FileReader(gridControllerPath))) {
			
			String eachLine;
			while((eachLine = reader.readLine())!=null) {
				lines.add(eachLine);
				if(eachLine.trim().replace(" ", "").equals("publicclassGridController{")) {
					lines.add(instanceForService);
					for(Map<String,String> map : list) {
						lines.add("");
						lines.add(map.get("annotation"));
						lines.add(map.get("methodSignature"));
						lines.add(map.get("methodBody"));
					}
				}
			}
			for(String s : lines) {
				System.out.println(s);
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Printing functionality
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(gridControllerPath))){
			for(String eachLine:lines) {
				writer.write(eachLine);
				writer.newLine();
				writer.flush();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void writeOnGridService(List<Map<String,String>> list) {
		ArrayList<String> allLines = new ArrayList<String>();
		String gridServicePath="F:\\New Git Clones\\ABCD_project_maker\\src\\main\\java\\com\\eit\\abcd\\projectmaker\\service\\GridService.java";
		String javaFunctionFilePath="F:\\GridDemoCode.txt";
		
		String otherLogics="";
		boolean isLineReached=false;
		boolean writeCondition=false;

		try(BufferedReader reader = new BufferedReader(new FileReader(javaFunctionFilePath))){
			String eachLine;
			while((eachLine = reader.readLine())!=null) {
				if(eachLine.trim().replaceAll(" ", "").equalsIgnoreCase("JSONArraycolumnArray=newJSONArray();")) {
					isLineReached=true;
				}
				if(isLineReached) {
					otherLogics+=eachLine+"\n";
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(otherLogics);
		for(int i=0;i<list.size();i++) {
			list.get(i).put("otherLogics", otherLogics); //adding other logics got from the java function file to all the maps in the list
		}
		//list.forEach(map -> System.out.println(map.toString()));
		
		try(BufferedReader reader = new BufferedReader(new FileReader(gridServicePath))){
			String eachLine;
			while((eachLine = reader.readLine())!=null) {
				allLines.add(eachLine);
				if(eachLine.trim().replaceAll(" ","").equalsIgnoreCase("privateStringschema;")) {
					for(int i=0;i<list.size();i++) {
						Map<String,String> map = list.get(i);
//						allLines.add(map.get("annotation"));
						allLines.add(map.get("methodSignature"));
						allLines.add(map.get("gridJsonVariable"));
						allLines.add(map.get("langVariable"));
						allLines.add(map.get("columnStyleVariable"));
						allLines.add(map.get("gridWidthVariable"));
						allLines.add(map.get("gridHeightVariable"));
						allLines.add(map.get("columnNamesVariable"));
						allLines.add(map.get("displayNamesVariable"));
						allLines.add(map.get("firstRowFilterVariable"));
						allLines.add(map.get("showgridbyrole"));
						allLines.add(map.get("datas"));
						allLines.add(map.get("otherLogics"));
					}
				}
				
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		for(String s : allLines) {
//			System.out.println(s);
//		}
		
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(gridServicePath))){
			
			for(int i=0;i<allLines.size();i++) {
				
				writer.write(allLines.get(i));
				writer.newLine();
				
			}
			writer.flush();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
