/*
 * TableNode.fx
 *
 * Created on May 18, 2009, 7:48:50 PM
 */

package c1democlient.ui;
/*
*  TableNode.fx -
*  A custom node that contains rows and columns, each cell
*  containing a node.
*
*  Developed 2008 by James L. Weaver (jim.weaver at lat-inc.com)
*  to demonstrate how to create custom nodes in JavaFX
*/

import javafx.scene.*;
import javafx.scene.input.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.*;

/*
* A custom node that contains rows and columns, each cell
* containing a node.  Column widths may be set individually,
* and the height of the rows can be set.  In addition, several
* other vars such as width and color of the scrollbar
* may be set.  The scrollbar will show only when necessary,
* and overlays the right side of each row, so the rightmost
* column should be given plenty of room to display data and
* a scrollbar.
*/
public class TableNode extends CustomNode {

  /*
   * Contains the height of the table in pixels.
   */
  public var height:Integer = 200;

  /*
   * Contains the height of each row in pixels.
   */
  public var rowHeight:Integer;

  /*
   * A sequence containing the column widths in pixels.  The
   * number of elements in the sequence determines the number of
   * columns in the table.
   */
  public var columnWidths:Integer[];

  /*
   * A sequence containing the nodes in the cells.  The nodes are
   * placed from left to right, continuing to the next row when
   * the current row is filled.
   */
  public var content:Node[];

  /*
   * The selected row number (zero-based)
   */
  public var selectedIndex:Integer;

  /*
   * The height (in pixels) of the space between rows of the table.
   * This space will be filled with the tableFill color.
   */
  public var rowSpacing:Integer = 1;

  /*
   * The background color of the table
   */
  public var tableFill:Paint;

  /*
   * The background color of an unselected even row
   */
  public var evenRowFill:Paint;

  /*
   * The background color of an unselected odd row
   */
  public var oddRowFill:Paint;

  /*
   * The background color of a selected row
   */
  public var selectedRowFill:Paint;

  /*
   * The color or gradient of the vertical scrollbar.
   */
  public var vertScrollbarFill:Paint = Color.BLACK;

  /*
   * The color or gradient of the vertical scrollbar thumb.
   */
  public var vertScrollbarThumbFill:Paint = Color.WHITE;

  /*
   * The width (in pixels) of the vertical scrollbar.
   */
  public var vertScrollbarWidth:Integer = 20;

  /*
   * The number of pixels from the left of a cell to place the node
   */
  var cellHorizMargin:Integer = 10;

  /*
   * Contains the width of the table in pixels.  This is currently a
   * calculated value based upon the specified column widths
   */
  var width:Integer = bind
    computePosition(columnWidths, sizeof columnWidths);

  function computePosition(sizes:Integer[], element:Integer) {
    var position = 0;
    if (sizeof sizes > 1) {
      for (i in [0..element - 1]) {
        position += sizes[i];
      }
    }
    return position;
  }

  /**
   * The onSelectionChange function var that is executed when the
   * a row is selected
   */
  public var onSelectionChange:function(row:Integer):Void;

  /**
   * Create the Node
   */
  public override function create():Node {
    var numRows = sizeof content / sizeof columnWidths;
    var tableContentsNode:Group;
    var needScrollbar:Boolean = bind (rowHeight + rowSpacing) * numRows  > height;
    Group {
      var thumbStartY = 0.0;
      var thumbEndY = 0.0;
      var thumb:Rectangle;
      var track:Rectangle;
      var rowRef:Group;
      content: [
        for (row in [0..numRows - 1], colWidth in columnWidths) {
          Group {
            transforms: bind
              Translate.translate(computePosition(columnWidths, indexof colWidth) +
                                  cellHorizMargin,
                                  ((rowHeight + rowSpacing) * row) + (-1.0 * thumbEndY *
                                  ((rowHeight + rowSpacing) * numRows) / height))
            content: bind [
              Rectangle {
                width: colWidth
                height: rowHeight
                fill: if (indexof row == selectedIndex)
                        selectedRowFill
                      else
                        if ( (indexof row) mod 2 == 0 )
                            evenRowFill
                        else
                            oddRowFill
              },
              Line {
                startX: 0
                startY: 0
                endX: colWidth
                endY: 0
                strokeWidth: rowSpacing
                stroke: tableFill
              },
              rowRef = Group {
                var node =
                  content[indexof row * (sizeof columnWidths) + indexof colWidth];
                transforms: bind Translate.translate(0, rowHeight / 2 -
                                                       node.layoutBounds.height / 2)
                content: node
              }
            ]
            onMouseClicked:
              function (me:MouseEvent) {
                selectedIndex = row;
                onSelectionChange(row);
              }
          }
        },
        // Scrollbar
        if (needScrollbar)
          Group {
            transforms: bind Translate.translate(width - vertScrollbarWidth, 0)
            content: [
              track = Rectangle {
                x: 0
                y: 0
                width: vertScrollbarWidth
                height: bind height
                fill: vertScrollbarFill
              },
              //Scrollbar thumb
              thumb = Rectangle {
                x: 0
                y: bind thumbEndY
                width: vertScrollbarWidth
                height: bind 1.0 * height / ((rowHeight + rowSpacing) * numRows) * height
                fill: vertScrollbarThumbFill
                arcHeight: 10
                arcWidth: 10
                onMousePressed: function(e:MouseEvent):Void {
                  thumbStartY = e.dragY - thumbEndY;
                }
                onMouseDragged: function(e:MouseEvent):Void {
                  var tempY = e.dragY - thumbStartY;
                  // Keep the scroll thumb within the bounds of the scrollbar
                  if (tempY >=0 and tempY + thumb.height <= track.height) {
                    thumbEndY = tempY;
                  }
                  else if (tempY < 0) {
                    thumbEndY = 0;
                  }
                  else {
                    thumbEndY = track.height - thumb.height;
                  }
                }
              }
            ]
          }
        else
          null
      ]
      clip:
        Rectangle {
          width: bind width
          height: bind height
        }
      onMouseWheelMoved: function(e:MouseEvent):Void {
        var tempY = thumbEndY + e.wheelRotation * 4;
        // Keep the scroll thumb within the bounds of the scrollbar
        if (tempY >=0 and tempY + thumb.height <= track.height) {
          thumbEndY = tempY;
        }
        else if (tempY < 0) {
          thumbEndY = 0;
        }
        else {
          thumbEndY = track.height - thumb.height;
        }
      }
    }
  }
}
