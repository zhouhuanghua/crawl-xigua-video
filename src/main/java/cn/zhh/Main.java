package cn.zhh;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 下载图片
 *
 * @author Zhou Huanghua
 */
@SuppressWarnings("all")
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        // 可以考虑多线程
        for(int i = 1; i < 600; i++) {
            try {
                download(i);
            } catch (IOException e) {
                System.out.println("第" + i + "张图片下载异常！");
                e.printStackTrace();
            }
        }
    }

    public static void download(int index) throws IOException {
        String url = "https://api.isoyu.com/uploads/beibei/beibei_" + String.format("%04d", index) + ".jpg";
        Connection.Response response = Jsoup.connect(url)
                .ignoreContentType(true)
                .maxBodySize(10 * 1024 * 1024)
                .execute();
        Files.write(Paths.get("C:/Users/SI-GZ-1766/Desktop/jpg/", index + ".jpg"), response.bodyAsBytes());
        System.out.println("第" + index + "张图片下载完成！");
    }
}
