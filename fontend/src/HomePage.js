import React from "react";
import Chart from "./Chart";
import { Selection } from "./Selection";
import { getData } from "./utils";
import Chip from "@material-ui/core/Chip";
import Snackbar from "@material-ui/core/Snackbar";
export class HomePage extends React.Component {
  state = {
    listSelected: [],
    data: [],
    showError: "",
  };

  onCloseError = () => {
    this.setState({ showError: "" });
  };

  handleSelectedOption = async (selectedOption) => {
    const { listSelected, data } = this.state;
    if (data.length === 4) {
      this.setState({ showError: "Không thể thêm quá 4 công ty!" });
      return;
    }
    const dataStockCompany = await getData(selectedOption.company_id);
    console.log(dataStockCompany);
    if (!dataStockCompany || dataStockCompany === []) return;
    this.setState({
      listSelected: [...listSelected, selectedOption],
      data: [...data, dataStockCompany],
    });
  };

  deletedSelected = (item, index) => {
    const { listSelected, data } = this.state;
    listSelected.splice(index, 1);
    data.splice(index, 1);
    this.setState({ listSelected, data });
  };

  render() {
    const { listSelected, data, showError } = this.state;
    let heightChart = 850,
      numCol = 1,
      length = data.length;
    if (length === 1) {
      heightChart = 850;
      numCol = 1;
    } else if (length === 2) {
      heightChart = 450;
      numCol = 1;
    } else if (length >= 3) {
      heightChart = 450;
      numCol = 2;
    }
    return (
      <div>
        <Snackbar
          open={showError !== ""}
          autoHideDuration={3000}
          anchorOrigin={{ vertical: "top", horizontal: "right" }}
          message={showError}
          onClose={this.onCloseError}
        />
        <div style={{ display: "flex", alignItems: "center" }}>
          <Selection
            className="selection"
            handleSelectedOption={this.handleSelectedOption}
          />
          {listSelected.map((item, index) => (
            <Chip
              style={{ marginLeft: 10 }}
              label={
                <a
                  href={`https://dstock.vndirect.com.vn/tong-quan/${item.code}`}
                  target="_blank"
                >
                  {item.code}
                </a>
              }
              onDelete={() => this.deletedSelected(item, index)}
            ></Chip>
          ))}
        </div>
        {length !== 0 && (
          <div className="chart">
            {data.map((item, index) => {
              return (
                <Chart
                  data={item}
                  height={heightChart}
                  numCol={numCol}
                  info={listSelected[index]}
                />
              );
            })}
          </div>
        )}
      </div>
    );
  }
}
