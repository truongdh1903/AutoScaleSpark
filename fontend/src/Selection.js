import React, { Component } from "react";

import AsyncSelect from "react-select/async";

import { searchCompany } from "./utils";

const colourOptions = [
  {
    beta: 0.77,
    change_per_year: 28.54,
    code: "VIC",
    company_id: 958739,
    country: "Vietnam",
    eps: 2169.75,
    market_cap: 3813399999999999.5,
    name: "Vingroup JSC",
    p_e_ratio: 61.62,
    revenue: 133784450000000.02,
    shares_outstanding: 3231660110,
    volume: 1729000,
    volume_3m: 2300000,
  },
  {
    beta: 1.37,
    change_per_year: 6.35,
    code: "BID",
    company_id: 958398,
    country: "Vietnam",
    eps: 1801.77,
    market_cap: 1.818e15,
    name: "Joint Stock Commercial Bank for Investment and Development of Vietnam",
    p_e_ratio: 22.76,
    revenue: 6.352839e13,
    shares_outstanding: 4.02201804e9,
    volume: 6390300.0,
    volume_3m: 4090000.0,
  },
  {
    beta: 0.61,
    change_per_year: 57.06,
    code: "MCH",
    company_id: 995126,
    country: "Vietnam",
    eps: 6483.65,
    market_cap: 7.96e14,
    name: "Masan Consumer Corp",
    p_e_ratio: 17.32,
    revenue: 2.407062e13,
    shares_outstanding: 7.08793818e8,
    volume: 2529.0,
    volume_3m: 28351.1,
  },
  {
    beta: 1.06,
    change_per_year: 125.36,
    code: "DIG",
    company_id: 958452,
    country: "Vietnam",
    eps: 2677.5,
    market_cap: 8.73e13,
    name: "Development Investment Construction JSC",
    p_e_ratio: 9.99,
    revenue: 2.98759e12,
    shares_outstanding: 3.46437961e8,
    volume: 5915700.0,
    volume_3m: 4940000.0,
  },
];

const customStyles = {
  container: () => ({
    width: 450,
    marginLeft: 70,
    zIndex: 1,
  }),
  menu: () => ({
    width: 450,
    position: "absolute",
    backgroundColor: "#FFF",
    zIndex: 9999,
  }),
};

export class Selection extends Component {
  constructor(props) {
    super(props);
    this.state = { inputValue: "", listSelected: [] };
  }

  handleInputChange = (newValue) => {
    const inputValue = newValue.replace(/\W/g, "");
    this.setState({ inputValue });
    // this.props.handleInputChange();
  };

  loadOptions = async (inputValue) => {
    const companies = await searchCompany(inputValue);
    return companies.map((item) => {
      return {
        ...item,
        label: `${item.code} - ${item.name}`,
      };
    });
  };

  handleSelectedOption = (selectedOption) => {
    this.setState({ inputValue: "" });
    this.props.handleSelectedOption(selectedOption);
  };

  render() {
    const { loadOptions, handleChange, handleInputChange, inputValue } =
      this.props;
    return (
      <div>
        <AsyncSelect
          styles={customStyles}
          loadOptions={this.loadOptions}
          onInputChange={this.handleInputChange}
          onChange={this.handleSelectedOption}
          value={this.state.inputValue}
          placeholder="Search company..."
        />
      </div>
    );
  }
}
