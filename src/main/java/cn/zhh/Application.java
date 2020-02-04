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
 * 修改“小视频首页”和“存放目录”之后运行main函数即可
 *
 * @author Zhou Huanghua
 */
@SuppressWarnings("all")
public class Application {

    /**
     * 小视频首页，按需修改
     */
    private static final String MAIN_PAGE_URL = "https://www.ixigua.com/home/3276166340814919/hotsoon/";

    /**
     * 存放目录，按需修改
     */
    private static final String FILE_SAVE_DIR = "C:/Users/SI-GZ-1766/Desktop/MP4/";

    /**
     * 线程池，按需修改并行数量。实际开发请自定义避免OOM
     */
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * 谷歌浏览器参数
     */
    private static final ChromeOptions CHROME_OPTIONS = new ChromeOptions();

    static {
        // 驱动位置
        System.setProperty("webdriver.chrome.driver", "src/main/resources/static/chromedriver.exe");
        // 避免被浏览器检测识别
        CHROME_OPTIONS.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
    }

    /**
     * main函数
     *
     * @param args 运行参数
     * @throws InterruptedException 睡眠中断异常
     */
    public static void main(String[] args) throws InterruptedException {
        // 获取小视频列表的div元素，批量处理
        Document mainDoc = Jsoup.parse(getMainPageSource());
        Elements divItems = mainDoc.select("div[class=\"BU-CardB UserDetail__main__list-item\"]");
        // 这里使用CountDownLatch关闭线程池，只是避免执行完一直没退出
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
        System.exit(0);
    }

    /**
     * 获取首页内容
     *
     * @return 首页内容
     * @throws InterruptedException 睡眠中断异常
     */
    private static String getMainPageSource() throws InterruptedException {
        ChromeDriver driver = new ChromeDriver(CHROME_OPTIONS);
        try {
            driver.get(MAIN_PAGE_URL);
            long waitTime = Double.valueOf(Math.max(3, Math.random() * 5) * 1000).longValue();
            TimeUnit.MILLISECONDS.sleep(waitTime);
            long timeout = 30_000;
            // 循环下拉，直到全部加载完成或者超时
            do {
                new Actions(driver).sendKeys(Keys.END).perform();
                TimeUnit.MILLISECONDS.sleep(waitTime);
                timeout -= waitTime;
            } while (!driver.getPageSource().contains("已经到底部，没有新的内容啦")
                    && timeout > 0);
            return driver.getPageSource();
        } finally {
            driver.close();
        }
    }

    /**
     * 处理每个小视频
     *
     * @param div 小视频div标签元素
     * @throws Exception 各种异常
     */
    private static void handleItem(Element div) throws Exception {
        String href = div.getElementsByTag("a").first().attr("href");
        String src = getVideoUrl("https://www.ixigua.com" + href);
        // 有些blob开头的（可能还有其它）暂不处理
        if (src.startsWith("//")) {
            Connection.Response response = Jsoup.connect("https:" + src)
                    // 解决org.jsoup.UnsupportedMimeTypeException: Unhandled content type. Must be text/*, application/xml, or application/xhtml+xml. Mimetype=video/mp4, URL=
                    .ignoreContentType(true)
                    // The default maximum is 1MB.
                    .maxBodySize(100 * 1024 * 1024)
                    .execute();
            Files.write(Paths.get(FILE_SAVE_DIR, href.substring(1) + ".mp4"), response.bodyAsBytes());
        } else {
            System.out.println("无法解析的src：[" + src + "]");
        }
    }

    /**
     * 获取小视频实际链接
     *
     * @param itemUrl 小视频详情页
     * @return 小视频实际链接
     * @throws InterruptedException 睡眠中断异常
     */
    private static String getVideoUrl(String itemUrl) throws InterruptedException {
        ChromeDriver driver = new ChromeDriver(CHROME_OPTIONS);
        try {
            driver.get(itemUrl);
            long waitTime = Double.valueOf(Math.max(5, Math.random() * 10) * 1000).longValue();
            long timeout = 50_000;
            Element v;
            /**
             * 循环等待，直到链接出来
             * ※这里可以考虑浏览器驱动自带的显式等待()和隐士等待
             */
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
