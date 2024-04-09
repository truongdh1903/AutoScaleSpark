import scrapy
import json
from InvestData.items import CompanyItem
from InvestData.settings import LOG_COUNTRY, ROOT_FOLDER
import urllib.parse


class CompanySpider(scrapy.Spider):
    name = 'company'
    allowed_domains = ['investing.com']
    headers = {
        'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36',
        'X-Requested-With': 'XMLHttpRequest',
    }
    url = 'https://www.investing.com/equities/StocksFilter?'
    custom_settings = {
        'ITEM_PIPELINES': {
            'InvestData.pipelines.CompanyPipeline': 400
        }
    }

    """
    Note:
    Edit only the feed() method to change the spider's input.
    feed() should yield a list of smlIds
    """

    def feed(self):
        # Open country list and parse one by one
        with open(ROOT_FOLDER + LOG_COUNTRY, 'r') as f:
            lines = f.readlines()

        for line in lines:
            yield json.loads(line)['country_id'],

    def start_requests(self):

        params = {
            'noconstruct': '1',
            'smlID': None,
            'tabletype': 'fundamental',
            'index_id': 'all',
        }

        for country_id in self.feed():
            country_id = list(country_id)
            country_id = ''.join(country_id)
            country_id.replace("'", "")
            country_id.replace(",", "")
            params['smlID'] = country_id
            params_str = urllib.parse.urlencode(params)
            yield scrapy.FormRequest(url=self.url + params_str, callback=self.parse, headers=self.headers, cb_kwargs={'smlId': country_id})

    def parse(self, response, smlId):

        for data in response.xpath("//table[@id='fundamental']/tbody/tr"):
            item = CompanyItem()
            item['name'] = data.xpath('./td[2]/span').attrib['data-name']
            item['company_id'] = data.xpath('./td[2]/span').attrib['data-id']
            item['volume_3m'] = data.xpath('./td[3]/text()').get()
            item['market_cap'] = data.xpath('./td[4]/text()').get()
            item['revenue'] = data.xpath('./td[5]/text()').get()
            item['p_e_ratio'] = data.xpath('./td[6]/text()').get()
            item['beta'] = data.xpath('./td[7]/text()').get()
            item['country'] = data.xpath('./td[1]/span').attrib['title']
            url = 'https://www.investing.com'
            url += data.xpath('./td[2]/a').attrib['href']
            req = scrapy.Request(
                url, callback=self.parse2, headers=self.headers)
            req.cb_kwargs['item'] = item
            yield req

    def parse2(self, response, item):
        name = response.xpath("//h1/text()").get()
        item['code'] = name[name.find("(")+1:name.find(")")]
        item["volume"] = response.xpath(
            "//dl[@data-test='key-info']/div[7]/dd/span/span[1]/text()").get()
        item["change_per_year"] = response.xpath(
            "//dl[@data-test='key-info']/div[13]/dd/span/span[1]/text()").get()
        item["shares_outstanding"] = response.xpath(
            "//dl[@data-test='key-info']/div[14]/dd/span/span[1]/text()").get()
        item["eps"] = response.xpath(
            "//dl[@data-test='key-info']/div[6]/dd/span/span[1]/text()").get()
        print(item)
        yield item
