import React from "react";
import PropTypes from "prop-types";

import { format } from "d3-format";
import { timeFormat } from "d3-time-format";

import { ChartCanvas, Chart } from "react-stockcharts";
import {
  BarSeries,
  BollingerSeries,
  CandlestickSeries,
  LineSeries,
} from "react-stockcharts/lib/series";
import { XAxis, YAxis } from "react-stockcharts/lib/axes";
import {
  CrossHairCursor,
  EdgeIndicator,
  CurrentCoordinate,
  MouseCoordinateX,
  MouseCoordinateY,
} from "react-stockcharts/lib/coordinates";

import { discontinuousTimeScaleProvider } from "react-stockcharts/lib/scale";
import { GroupTooltip, HoverTooltip } from "react-stockcharts/lib/tooltip";
import { ema, bollingerBand } from "react-stockcharts/lib/indicator";
import { fitWidth } from "react-stockcharts/lib/helper";
import { last } from "react-stockcharts/lib/utils";

import "./index.css";

const bbAppearance = {
  stroke: {
    top: "#964B00",
    middle: "#FF6600",
    bottom: "#964B00",
  },
  fill: "#4682B4",
};

const dateFormat = timeFormat("%d-%m-%Y");
const numberFormat = format(",.0f"); // ,.1f

function tooltipContent(ys) {
  return ({ currentItem, xAccessor }) => {
    return {
      x: dateFormat(xAccessor(currentItem)),
      y: [
        {
          label: "open",
          value: currentItem.open && numberFormat(currentItem.open),
        },
        {
          label: "high",
          value: currentItem.high && numberFormat(currentItem.high),
        },
        {
          label: "low",
          value: currentItem.low && numberFormat(currentItem.low),
        },
        {
          label: "close",
          value: currentItem.close && numberFormat(currentItem.close),
        },
      ]
        .concat(
          ys.map((each) => ({
            label: each.label,
            value: each.value(currentItem),
            stroke: each.stroke,
          }))
        )
        .filter((line) => line.value),
    };
  };
}

class CandleStickChartWithDarkTheme extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    const {
      height,
      width,
      numCol,
      type,
      data: initialData,
      ratio,
      info,
    } = this.props;

    const margin = { left: 70, right: 70, top: 20, bottom: 30 };

    const gridHeight = height - margin.top - margin.bottom;
    const gridWidth = width - margin.left - margin.right;

    const showGrid = true;
    const yGrid = showGrid
      ? { innerTickSize: -1 * gridWidth, tickStrokeOpacity: 0.2 }
      : {};
    const xGrid = showGrid
      ? { innerTickSize: -1 * gridHeight, tickStrokeOpacity: 0.2 }
      : {};

    const ema20 = ema()
      .id(0)
      .options({ windowSize: 20 })
      .merge((d, c) => {
        d.ema20 = c;
      })
      .accessor((d) => d.ema20);

    const ema50 = ema()
      .id(2)
      .options({ windowSize: 50 })
      .merge((d, c) => {
        d.ema50 = c;
      })
      .accessor((d) => d.ema50);

    const bb = bollingerBand()
      .merge((d, c) => {
        d.bb = c;
      })
      .accessor((d) => d.bb);

    const calculatedData = bb(ema20(ema50(initialData)));
    const xScaleProvider = discontinuousTimeScaleProvider.inputDateAccessor(
      (d) => d.date
    );
    const { data, xScale, xAccessor, displayXAccessor } =
      xScaleProvider(calculatedData);

    const start = xAccessor(last(data));
    const end = xAccessor(data[Math.max(0, data.length - 150)]);
    const xExtents = [start, end];

    console.log(info);
    return (
      <ChartCanvas
        height={height}
        width={width / numCol}
        ratio={ratio}
        margin={margin}
        type={type}
        seriesName="MSFT"
        data={data}
        xScale={xScale}
        xAccessor={xAccessor}
        displayXAccessor={displayXAccessor}
        xExtents={xExtents}
      >
        <Chart
          id={1}
          height={height - 50} //400,800
          yExtents={[
            (d) => [d.high, d.low],
            bb.accessor(),
            ema20.accessor(),
            ema50.accessor(),
          ]}
          padding={{ top: 10, bottom: 20 }}
        >
          <YAxis
            axisAt="right"
            orient="right"
            ticks={5}
            {...yGrid}
            inverted={true}
            tickStroke="#FFFFFF"
            tickFormat={numberFormat}
          />
          <XAxis
            axisAt="bottom"
            orient="bottom"
            {...xGrid}
            tickStroke="#FFFFFF"
            stroke="#FFFFFF"
          />

          <MouseCoordinateY
            at="right"
            orient="right"
            displayFormat={numberFormat}
          />
          <MouseCoordinateX
            at="bottom"
            orient="bottom"
            displayFormat={dateFormat}
          />

          <CandlestickSeries
            stroke={(d) => (d.close > d.open ? "#6BA583" : "#DB0000")}
            wickStroke={(d) => (d.close > d.open ? "#6BA583" : "#DB0000")}
            fill={(d) => (d.close > d.open ? "#6BA583" : "#DB0000")}
          />

          <LineSeries yAccessor={ema20.accessor()} stroke={ema20.stroke()} />
          <LineSeries yAccessor={ema50.accessor()} stroke={ema50.stroke()} />

          <BollingerSeries yAccessor={(d) => d.bb} {...bbAppearance} />

          <CurrentCoordinate
            yAccessor={ema20.accessor()}
            fill={ema20.stroke()}
          />
          <CurrentCoordinate
            yAccessor={ema50.accessor()}
            fill={ema50.stroke()}
          />

          <EdgeIndicator
            itemType="last"
            orient="right"
            edgeAt="right"
            yAccessor={(d) => d.close}
            fill={(d) => (d.close > d.open ? "#6BA583" : "#DB0000")}
            displayFormat={numberFormat}
          />

          {/* Text Header: Info about company */}
          <GroupTooltip
            layout="horizontal"
            width={100}
            onClick={(e) => console.log(e)}
            options={[
              {
                yAccessor: () => 0,
                yLabel: `ID: ${info.company_id} - CODE: ${
                  info.code
                } - VOLUME: ${numberFormat(info.volume)}`,
                valueFill: "#303030",
                labelFill: "#FFF",
              },
            ]}
          />

          <HoverTooltip
            yAccessor={ema50.accessor()}
            tooltipContent={tooltipContent([
              {
                label: `${ema20.type()}(${ema20.options().windowSize})`,
                value: (d) => numberFormat(ema20.accessor()(d)),
                stroke: ema20.stroke(),
              },
            ])}
            fontSize={15}
          />
        </Chart>

        {/* Bar Chart: volume */}
        <Chart
          id={2}
          yExtents={(d) => d.volume}
          height={250}
          origin={(w, h) => [0, h - 250]}
        >
          <YAxis
            axisAt="left"
            orient="left"
            ticks={5}
            tickFormat={format(".2s")}
            tickStroke="#FFFFFF"
          />
          <BarSeries
            yAccessor={(d) => d.volume}
            fill={(d) => (d.close > d.open ? "#6BA583" : "#DB0000")}
          />
        </Chart>

        <CrossHairCursor stroke="#FFFFFF" />
      </ChartCanvas>
    );
  }
}
CandleStickChartWithDarkTheme.propTypes = {
  data: PropTypes.array.isRequired,
  width: PropTypes.number.isRequired,
  ratio: PropTypes.number.isRequired,
  type: PropTypes.oneOf(["svg", "hybrid"]).isRequired,
};

CandleStickChartWithDarkTheme.defaultProps = {
  type: "svg",
};
CandleStickChartWithDarkTheme = fitWidth(CandleStickChartWithDarkTheme);

export default CandleStickChartWithDarkTheme;
