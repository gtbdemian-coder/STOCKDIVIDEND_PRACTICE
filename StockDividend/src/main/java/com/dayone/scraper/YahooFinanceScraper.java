package com.dayone.scraper;

import com.dayone.model.Company;
import com.dayone.model.Dividend;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.Month;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Component
public class YahooFinanceScraper implements Scraper {

    private static final String STATISTICS_URL = "https://finance.yahoo.com/quote/%s/history/?period1=%d&period2=%d&frequency=1mo";
    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s?p=%s";


    private static final long START_TIME = 86400; // 60 * 60 * 24

    @Override
    public ScrapedResult scrap(Company company) {

    var scrapResult = new ScrapedResult();
    scrapResult.setCompany(company);

    try {
        long now = System.currentTimeMillis() / 1000;
        String url = String.format(STATISTICS_URL, company.getTicker(), START_TIME, now);
        Connection connection = Jsoup.connect(url);
        //코드 작성 사유: Error 503가 지속적으로 발생함에 따라, HTTP 요청 시 사용자가 '사람'이라는 것을 증명하기 위해 아래의 코드를 추가하였습니다.
        Document document = connection
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        //코드 작성 사유: HTML 구조가 변화됨에 따라, 현재 HTML에 맞게 코드를 구현해보았습니다.
        Elements parsingDivs = document.select("table.table.svelte-ewueuo tbody > tr");

        List<Dividend> dividends = new ArrayList<>();
        for (Element e : parsingDivs) {
            String txt = e.text();
            if (!txt.contains("Dividend")) {
                continue;
            }

            String[] splits = txt.split(" ");
            if (splits.length < 4) {
                throw new RuntimeException("Unexpected format for dividend data: " + txt);
            }

            int month = Month.strToNumber(splits[0]);
            int day = Integer.parseInt(splits[1].replace(",", ""));
            int year = Integer.parseInt(splits[2]);
            String dividend = splits[3];

            if (month < 0) {
                throw new RuntimeException("Unexpected Month enum value -> " + splits[0]);
            }

            dividends.add(new Dividend(LocalDateTime.of(year, month, day, 0, 0), dividend));

        }
        scrapResult.setDividends(dividends);

        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }

        return scrapResult;
    }

    @Override
    public Company scrapCompanyByTicker(String ticker) {
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try {
            //코드 작성 사유: Error 503가 지속적으로 발생함에 따라, HTTP 요청 시 사용자가 '사람'이라는 것을 증명하기 위해 아래의 코드를 추가하였습니다.
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            //코드 작성 사유: 수업 속에 사용된 코드가 작동하지 않아, HTML 구조를 분석후 새로운 코드를 사용하였습니다.
            Elements parsingDivs = document.select("h1.svelte-3a2v0c");

            if (!parsingDivs.isEmpty()) {
                String HTMLText = parsingDivs.get(0).text();
                String title = HTMLText.split(" \\(")[0].trim();
                ticker = HTMLText.split(" \\(")[1].replace(")", "").trim();

                return new Company(ticker, title);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}


