# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://docs.scrapy.org/en/latest/topics/item-pipeline.html


# useful for handling different item types with a single interface
from itemadapter import ItemAdapter
from .settings import LOG_COMPANY, LOG_COUNTRY, LOG_STOCK, ROOT_FOLDER
import json


class DataPipeline:
    file_name = 'items.json'

    def open_spider(self, spider):
        self.file = open(self.file_name, 'a')

    def close_spider(self, spider):
        self.file.close()

    def process_item(self, item, spider):
        line = json.dumps(ItemAdapter(item).asdict()) + "\n"
        self.file.write(line)
        return item


class StockPipeline(DataPipeline):
    file_name = ROOT_FOLDER + LOG_STOCK


class CountryPipeline(DataPipeline):
    file_name = ROOT_FOLDER + LOG_COUNTRY


class CompanyPipeline(DataPipeline):
    file_name = ROOT_FOLDER + LOG_COMPANY
