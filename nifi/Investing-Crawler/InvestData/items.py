# Define here the models for your scraped items
#
# See documentation in:
# https://docs.scrapy.org/en/latest/topics/items.html

import scrapy


class DailyStockItem(scrapy.Item):
    date = scrapy.Field()
    close = scrapy.Field()
    open = scrapy.Field()
    high = scrapy.Field()
    low = scrapy.Field()
    volume = scrapy.Field()
    change = scrapy.Field()
    company_id = scrapy.Field()


class CompanyItem(scrapy.Item):
    name = scrapy.Field()
    short_name = scrapy.Field()
    country = scrapy.Field()
    volume_3m = scrapy.Field()
    volume = scrapy.Field()
    market_cap = scrapy.Field()
    revenue = scrapy.Field()
    p_e_ratio = scrapy.Field()
    beta = scrapy.Field()
    company_id = scrapy.Field()
    country_id = scrapy.Field()
    code = scrapy.Field()
    change_per_year = scrapy.Field()
    shares_outstanding = scrapy.Field()
    eps = scrapy.Field()


class CountryItem(scrapy.Item):
    name = scrapy.Field()
    url = scrapy.Field()
    country_id = scrapy.Field()
