package com.dd;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class LinkText {
	public static WebDriver driver;
	public static void main(String[] args) {
		int count=0;
		System.setProperty("webdriver.chrome.driver","C:\Users\babpandey\Downloads\OSV_Trainingmaterial\chromedriver.exe");
		driver=new ChromeDriver();
		driver.get("https://hdfcbank.com/");
		List<WebElement>links=driver.findElements(By.tagName("a"));
		System.out.println(links.size());
		for(int i=0;i<links.size();i++) {
			if(!links.get(i).getText().isEmpty()) {
			System.out.println(links.get(i).getText().toString());
			links=driver.findElements(By.tagName("a"));
			count++;
			}
		}
		System.out.println("The total visible links are "+count);

	}

}