import { timeParse } from "d3-time-format";
const axios = require("axios");

const parseDate = timeParse("%Y-%m-%d %H:%M:%S");

export const searchCompany = async (company = "VIC") => {
  try {
    const companies = await axios.post(
      "http://127.0.0.1:9200/company/_search",
      {
        from: 0,
        size: 10,
        query: {
          multi_match: {
            query: company,
            fields: ["name", "code"],
          },
        },
      }
    );
    if (!companies.data || !companies.data.hits || !companies.data.hits.hits)
      return [];
    return companies.data.hits.hits.map((item) => item._source);
  } catch (error) {
    console.log(error);
    return [];
  }
};

export const getData = async (company_id = 958397) => {
  try {
    const data = await axios.post("http://127.0.0.1:9200/stock/_search", {
      from: 0,
      size: 5000,
      query: {
        match: {
          company_id,
        },
      },
    });

    return data.data.hits.hits
      .map((item) => {
        return { ...item._source, date: parseDate(item._source.date) };
      })
      .sort((a, b) => Date.parse(a.date) - Date.parse(b.date));
  } catch (error) {
    console.log(error);
    return [];
  }
};
