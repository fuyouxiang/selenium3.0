package com.SeleniumLib.test;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;




public class test1 {
	public static void main(String[]args){
	    
        System.out.println("start selenium");
///////////如下为对百度网页进行一次搜索的过程；///////////

        WebDriver   driver;
        System.setProperty("webdriver.gecko.driver","D:\\工作\\selenium3\\geckodriver-v0.11.0-win64\\geckodriver.exe");     
        driver = new FirefoxDriver();
        driver.manage().window().maximize();
        driver.get("http://www.baidu.com/"); 
        ///////通过元素属性id=kw找到百度输入框，并输入"Selenium java"；
        driver.findElement(By.id("kw")).sendKeys("Selenium java");
        ///////通过元素属性id=su找到百度一下搜索按钮，并对按钮进行点击操作；
        driver.findElement(By.id("su")).click();
        ///////driver.close();  //暂时注释掉
    }
}
