import scrapy
from scrapy.spiders import CrawlSpider, Rule
from scrapy.linkextractors import LinkExtractor

# enable file pipeline
# ITEM_PIPELINES = {'scrapy.pipelines.files.FilesPipeline': 1}


class ApkUrlItem(scrapy.Item):
    file_url = scrapy.Field()
    name = scrapy.Field()


class ApkSpider(CrawlSpider):
    name = 'fdroid'
    allowed_domains = ['f-droid.org']
    start_urls = ['https://f-droid.org/en/packages/']

    rules = (
        Rule(LinkExtractor(allow=(r'packages/\d+/'))),

        # Extract links matching 'item.php' and parse them with the spider's method parse_item
        Rule(LinkExtractor(allow=('packages', )), callback='parse_item'),
    )

    def parse_item(self, response):
        self.logger.info('Hi, this is an package page! %s', response.url)
        item = ApkUrlItem()
        item['file_url'] = response.selector.xpath("//a[contains(text(), 'Download APK')]/@href").extract_first()
        item['name'] = response.css('title::text').re(r' (.*) \|')
        return item
