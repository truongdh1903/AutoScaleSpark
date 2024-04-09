import scrapy
import json
from InvestData.items import DailyStockItem
from InvestData.settings import LOG_COMPANY, ROOT_FOLDER


class StockSpider(scrapy.Spider):
    name = 'stock'
    allowed_domains = ['investing.com']
    headers = {
        'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36',
        'X-Requested-With': 'XMLHttpRequest',
    }
    start_url = 'https://www.investing.com/instruments/HistoricalDataAjax'

    custom_settings = {
        'ITEM_PIPELINES': {
            'InvestData.pipelines.StockPipeline': 400
        }
    }

    """
    Note:
    Edit only the feed() method to change the spider's input.
    feed() should yield a list of currIds
    """

    def feed(self):
        # Open country list and parse one by one
        with open(ROOT_FOLDER + LOG_COMPANY, 'r') as f:
            lines = f.readlines()

        for line in lines:
            yield json.loads(line)['company_id']

    """
    The following methods should not be changed in anyway, unless you know exactly what you are doing.
    """

    def start_requests(self):

        body = {
            'curr_id': '',
            'st_date': '01/01/2020',
            'end_date': '06/13/2021',
            'interval_sec': 'Daily',
            'sort_col': 'date',
            'sort_ord': 'DESC',
            'action': 'historical_data',
        }

        for company_id in self.feed():
            body['curr_id'] = company_id
            body_str = '&'.join('{}={}'.format(key, value)
                                for key, value in body.items())
            yield scrapy.FormRequest(url=self.start_url, callback=self.parse, headers=self.headers, formdata=body, cb_kwargs={'currId': company_id})

    def parse(self, response, currId):
        item = DailyStockItem()
        for data in response.xpath("//table[@id='curr_table']/tbody/tr"):
            item['date'] = data.xpath('./td[1]').attrib['data-real-value']
            item['close'] = data.xpath('./td[2]').attrib['data-real-value']
            item['open'] = data.xpath(
                './td[3]').attrib['data-real-value']
            item['high'] = data.xpath('./td[4]').attrib['data-real-value']
            item['low'] = data.xpath('./td[5]').attrib['data-real-value']
            item['volume'] = data.xpath('./td[6]').attrib['data-real-value']
            item['change'] = data.xpath('./td[7]/text()').get()
            item['company_id'] = currId
            yield item
