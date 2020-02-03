package cn.zhh;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Keys;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 主类
 *
 * @author Zhou Huanghua
 */
@SuppressWarnings("all")
public class Application {

    /** 小视频首页 */
    private static final String MAIN_PAGE_URL = "https://www.ixigua.com/home/95751659750/hotsoon/";

    /** 存放目录 */
    private static final String FILE_SAVE_DIR = "C:/Users/SI-GZ-1766/Desktop/MP4/";

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final ChromeOptions CHROME_OPTIONS = new ChromeOptions();

    static {
        System.setProperty("webdriver.chrome.driver", "src/main/resources/static/chromedriver.exe");
        CHROME_OPTIONS.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
    }

    public static void main(String[] args) throws InterruptedException {
        Document mainDoc = Jsoup.parse(getMainPageSource());
        Elements divItems = mainDoc.select("div[class=\"BU-CardB UserDetail__main__list-item\"]");
        CountDownLatch countDownLatch = new CountDownLatch(divItems.size());
        divItems.forEach(item ->
            EXECUTOR.execute(() -> {
                try {
                    Application.handleItem(item);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            })
        );
        countDownLatch.await();
        EXECUTOR.shutdown();
    }

    private static String getMainPageSource() throws InterruptedException {
        ChromeDriver driver = new ChromeDriver(CHROME_OPTIONS);
        try {
            driver.get(MAIN_PAGE_URL);
            long waitTime = Double.valueOf(Math.max(3, Math.random() * 5) * 1000).longValue();
            TimeUnit.MILLISECONDS.sleep(waitTime);
            long timeout = 30_000;
            do {
                new Actions(driver).sendKeys(Keys.END).perform();
                TimeUnit.MILLISECONDS.sleep(waitTime);
                timeout -= waitTime;
            } while (! driver.getPageSource().contains("已经到底部，没有新的内容啦") && timeout > 0);
            return driver.getPageSource();
        } finally {
            driver.close();
        }
    }

    private static void handleItem(Element div) throws Exception {
        String href = div.getElementsByTag("a").first().attr("href");
        String src = getVideoUrl("https://www.ixigua.com" + href);
        if (src.startsWith("//")) {
            Connection.Response response = Jsoup.connect("https:" + src)
                    .ignoreContentType(true)
                    .execute();
            Files.write(Paths.get(FILE_SAVE_DIR, href.substring(1) + ".mp4"), response.bodyAsBytes());
        } else {
            System.out.println("无法解析的src：[" + src + "]");
        }
    }

    private static String getVideoUrl(String itemUrl) throws InterruptedException {
        ChromeDriver driver = new ChromeDriver(CHROME_OPTIONS);
        try {
            driver.get(itemUrl);
            long waitTime = Double.valueOf(Math.max(5, Math.random() * 10) * 1000).longValue();
            long timeout = 50_000;
            Element v;
            do {
                TimeUnit.MILLISECONDS.sleep(waitTime);
                timeout -= waitTime;
            } while ((Objects.isNull(v = Jsoup.parse(driver.getPageSource()).getElementById("vs"))
                        || Objects.isNull(v = v.getElementsByTag("video").first()))
                    && timeout > 0);

            return v.attr("src");
        } finally {
            driver.close();
        }
    }
}
