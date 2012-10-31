// ident "@(#)tree.js 1.19 04/09/02 SMI"
//
// Copyright 2004 by Sun Microsystems, Inc. All rights reserved.
// Use is subject to license terms.
//
// This javascript file defines various javascript objects used by the client
// side tree component

/*****************************************************************************/
/* Define a tree node base class                                             */
/*****************************************************************************/

function CCAbstractTreeNode(label, statusTxt, url, icon, uid, iconWidth, onClick, target) {
    // every node will be assigned a unique index in the tree list of elements
    this.id = -1;
    // every node has a label
    this.label = label;
    // the node's target
    this.targ = target;
    // every node has status text
    this.statusTxt = statusTxt;
    // every node has an associated url
    this.url = url;
    // width of this node's icon
    this.iconWidth = iconWidth == -1 ? CCTREE_NODE_IMG_WIDTH : iconWidth;
    // every node has an icon
    this.icon = "<img height=\"19\" width=\"" + this.iconWidth + "\" src=\"" +
        icon + "\" border=\"0\" alt=\"" + this.label +
        "\" title=\"" + this.label + "\" align=\"middle\"></img>";
    // every node is represented by an html "layer"
    this.layer = null;
    // every node has a unique yoke to key
    this.key = uid;
    // every node can have a parent
    this.parent = null;
    // whether or not this is a "root" node (for display purposes)
    this.isRoot = false;
    // the tree this node is part of
    this.tree = null;
    // save onclick
    this.onClick = onClick; 
}

// This OO function maps to CCAbstractTreeNode::createIndex()
//
// It will be called when an item is created in order to add it to the tree's
// array of items / assign this node an id
CCAbstractTreeNode.prototype.createIndex = function() {
    this.id = this.tree.createIndex(this);
}

// This OO function maps to CCAbstractTreeNode::display()
//
// It will be called to display an item
CCAbstractTreeNode.prototype.doDisplay = function() {
    if (!this.isVisible()) {
        return;
    }

    if (is_nav4up) {
        this.layer.visibility = "show";
        // also show any sub layers
        if (this.tree.selectedId == this.id) {
            // show the selected / bold label sub layer
            this.layer.document.layers["sLabel"].visibility = "show";
        } else {
            // show the non selected / bold label sub layer
            this.layer.document.layers["label"].visibility = "show";
        }
    } else if (is_ie4up) {
        this.layer.setAttribute("style", "display:");
        this.layer.style.display = "";
        // test if this node is selected
        if (this.tree.selectedId == this.id) {
            this.layer.style.fontWeight = "bold";
            doc.getElementById("row" + this.id).className = CCTREE_SELECTED_ROW;
        }
    }
}

// This OO function maps to CCAbstractTreeNode::doHide()
//
// It will be called to hide an item
CCAbstractTreeNode.prototype.doHide = function() {
    // hide the table / layer containing this node
    if (is_nav4up) {
        this.layer.visibility = "hidden";
        for (i = 0; i < this.layer.document.layers.length; i++) {
            // also hide the nested label layers
            var sLayer = this.layer.document.layers[i];
            sLayer.visibility = "hidden";
        }
    } else if (is_ie4up) {
        this.layer.setAttribute('style', 'display:none');
        this.layer.style.display = "none";
    }
}

// This OO function maps to CCAbstractTreeNode::isTopNode()
//
// Returns true if this node is at the top of its tree
CCAbstractTreeNode.prototype.isTopNode = function() {
    if (!this.tree) {
        // this node is not part of any tree yet
        return false;
    }

    // test if this node is the first root level node of its tree
    if (this.id == this.tree.rootLevelNodes[0].id) {
        return true;
    }
    
    return false;
}

// This OO function maps to CCAbstractTreeNode::isVisible()
//
// Returns true if this node is currently visible, false otherwise
CCAbstractTreeNode.prototype.isVisible = function() {
    var p = this.parent;

    if (p == null) {
        // node has no parent, it's visible
        return true;
    }

    var pOpen = p.isOpen;

    if (!pOpen) {
        // parent is closed, can't be visible
        return false;
    } else {
        // parent is open, might be visible so check next ancestor
        return p.isVisible();
    }
}

// This OO function maps to CCAbstractTreeNode::getOpenAnchor()
//
// This method outputs the html necessary for this nodes link
CCAbstractTreeNode.prototype.getOpenAnchor = function(setId) {
    var anchor = "<a class=\"" + CCTREE_LINK_STYLE + "\" href=\"" + this.url;
    var customJs = "";
    if (this.onClick != null) {
       customJs = this.onClick;
    } 
    anchor += "\" onclick=\"javascript:" + customJs + "; " + this.tree.name + ".doSelection(";
    anchor += this.id + "); return true;\" onmouseover=\"window.status='";
    anchor += this.statusTxt + "'; return true;\" onmouseout=\"window.status";
    anchor += " = ''; return true;\" onblur=\"window.status = ''; ";
    anchor += "return true;\" target=\"" + this.targ + "\" ";
    if (setId) {
        anchor += "id=\"labelfor" + this.id + "\" ";
    }

    anchor += "title=\"" + this.statusTxt + "\">";

    return anchor;
}

// This OO function maps to CCAbstractTreeNode::getTableStart(bgColor)
//
// This method outputs the table start tag for a node with the given bgColor
CCAbstractTreeNode.prototype.getTableStart = function(bgColor, rowHeight) {
    var tableStart = "";

    if (is_nav4up) {
        // use the layer tag in ns4 - create one for the node's table
        tableStart += "\n<layer id=\"node" + this.id + "\" top=\"" + doc.yPos;
        tableStart += "\" visibility=\"show\">";
    }

    // each node goes into a table
    tableStart += "\n<table width=\"100%\" ";
    
    if (is_ie4up) {
        // ie and other newer browsers allow dhtml for any tag
        tableStart += "id=\"node" + this.id + "\" ";
    }

    if (bgColor != null) {
        tableStart += "bgcolor=\"" + bgColor + "\" ";
    }
    
    tableStart += "border=\"0\" cellspacing=\"0\" cellpadding=\"0\">";
    tableStart += "<tr";

    if (is_ie4up) {
        tableStart += " id=\"row" + this.id + "\" ";
    }

    if (rowHeight != null) {
        tableStart += " height=\"" + rowHeight + "\"";
    }

    if (this.isRoot) {
        tableStart += " class=\"" + CCTREE_ROOT_ROW_STLYE + "\"";
    }

    tableStart += ">" + CCTREE_TD + "<img src=\"" + CCTREE_BLANK_SRC + "\"";
    tableStart += " width=\"" + CCTREE_SPACER_WIDTH + "\" ";
    tableStart += "height=\"22\" alt=\"\"></img>";
    
    return tableStart;
}

/*****************************************************************************/
/* Define a container node class that extends the abstract node class        */
/*****************************************************************************/

function CCContainerNode(label, statusTxt, url, icon, uid, iconWidth, onClick, target) {
    // this class extends AbstractTreeNode
    this.base = CCAbstractTreeNode;
    // call the super constructor
    this.base(label, statusTxt, url, icon, uid, iconWidth, onClick, target);
    // flag indiciating if this is a last sibling
    this.isLastSibling = false;
    // pointer to the img element for the container's turner
    this.turnerImgObj = null;
    // whether or not this container is currently open
    this.isOpen = true;
    // this containers children
    this.children = new Array;
    // the number of children this container has
    this.numChildren = 0;
}

// CCContainerNode extends CCAbstractTreeNode
CCContainerNode.prototype = new CCAbstractTreeNode;

// This OO function maps to CCContainerNode::addChild(childNode)
//
// This method adds the given child to this container node
CCContainerNode.prototype.addChild = function(childNode) {
    // add the child node to the child array and increment our child count
    this.children[this.numChildren++] = childNode;
    childNode.parent = this;
}

// This OO function maps to CCContainerNode::collapseAll()
//
// This method closes this container as well as any descendant containers
CCContainerNode.prototype.collapseAll = function() {
    var i;

    for (i = 0; i < this.children.length; i++) {
        if (this.children[i].children) {
            this.children[i].collapseAll();
        }
    }

    this.setOpen(false);
}

// This OO function maps to CCContainerNode::display()
//
// This method displays this node
CCContainerNode.prototype.display = function() {
    if (!this.isVisible()) {
        // this node isn't currently visible
        return;
    }

    var i = 0;
    var showParentOfSelection = false;

    this.doDisplay();
    
    if (this.isOpen) {
        // this container is open, display all children too
        for (i = 0; i < this.children.length; i++) {
            this.children[i].display();
		}
    } else if (this.tree.selectedId != -1 && this.id != this.tree.selectedId &&
            this.hasDescendant(this.tree.selectedId)) {
        // this container is closed and some descendant is selected
        showParentOfSelection = true;
    }

    // show parent of selection style if necessary
    if (showParentOfSelection && !this.isRoot) {
        if (is_nav4up) {
            this.layer.document.layers["label"].visibility = "hidden";
            this.layer.document.layers["pLabel"].visibility = "show";
            p = this.parent;
            while (p != null && p.layer != null) {
                p.layer.document.layers["label"].visibility = "show";
                p.layer.document.layers["pLabel"].visibility = "hidden";
                p = p.parent;
            }
        } else {
            this.layer.style.fontWeight = "bold";
            p = this.parent;
            while (p != null && p.layer != null) {
                p.layer.style.fontWeight = "";
                p = p.parent;
            }
        }
    }
}

// This OO function maps to CCContainerNode::getHeightOfChildren()
//
// This method is used in netscape 4 to determine the total height of a
// container node and any descendant container nodes it may have
CCContainerNode.prototype.getHeightOfChildren = function() {
    var i;
    var height = 0;

    if (this.isOpen) {
        for (i = 0; i < this.children.length; i++) {
            height += this.children[i].layer.clip.height;
            if (this.children[i].children) {
                height += this.children[i].getHeightOfChildren();
            }
        }
    }

    return height;
}

// This OO function maps to CCContainerNode:getTurnerHtml(turnerSrc, turnerAlt)
//
// This method will return the html necessary to display this node's clickable
// turner using the image src passed in
CCContainerNode.prototype.getTurnerHtml = function(turnerSrc, turnerAlt) {
    if (turnerSrc == null || turnerSrc == "") {
        // no image src was specified
        return "";
    }
    
    return "<a onmouseover=\"window.status='" + this.statusTxt +
        "'; return true;\" onmouseout=\"window.status = ''; return true;\" " +
        "onblur=\"window.status = ''; return true;\" class=\"" +
        CCTREE_LINK_STYLE + "\" " + "href=\"javascript:" + this.tree.name +
        ".openOrClose(" + this.id + ");\"><img name=\"turnerIcon" + this.id +
        "\" id=\"turnerIcon" + this.id + "\" src=\"" + turnerSrc +
        "\" width=\"16\" height=\"22\" border=\"0\" alt=\"" + turnerAlt +
        "\" title=\"" + turnerAlt + "\"></a>";
}


// This OO function maps to CCContainerNode:hasDescendant(nodeId)
//
// This method will return true if a node with the given id is a descendant of
// this node
CCContainerNode.prototype.hasDescendant = function(nodeId) {
    if (this.id == nodeId) {
        return true;
    }

    var i = 0;
    var found = false;

    for (i = 0; i < this.numChildren; i++) {
        if (this.children[i].id == nodeId) {
            found = true;
            break;
        }
        if (this.children[i].children) {
            found = this.children[i].hasDescendant(nodeId);
            if (found) {
                break;
            }
        }
    }

    return found;
}

// This OO function maps to CCContainerNode:hide()
//
// This method will hide the container as well as any children
CCContainerNode.prototype.hide = function() {
    this.doHide();

    var i = 0;
    
    // also hide all children
    for (i = 0; i < this.children.length; i++) {
        this.children[i].hide();
    }
}

// This OO function maps to CCContainerNode:init(depth, lastSibling, lhsHtml)
//
// This method is called to initialize and display a container node 
CCContainerNode.prototype.init = function(depth, lastSibling, lhsHtml) {
    var i = 0;
    var turnerHtml;

    this.createIndex();
    
    if (depth > 0) {
        if (lastSibling) {
            // render this as the last sibling in the children array
            if (this.isTopNode()) {
                // top & non-root node
                turnerHtml = this.getTurnerHtml(CCTREE_OPEN_TOP_NS_SRC,
                    CCTREE_TURNER_OPEN_ALTTXT);
            } else {
                turnerHtml = this.getTurnerHtml(CCTREE_OPEN_LAST_SRC,
                    CCTREE_TURNER_OPEN_ALTTXT);
            }

            this.render(lhsHtml + turnerHtml, depth);

            // add a blank space imager to the left hand side html
            lhsHtml += CCTREE_BLANK_IMG;

            // store the fact that we're the last sibling
            this.isLastSibling = true;
        } else {
            // render this node as a middle sibling
            if (this.isTopNode()) {
                // top & non-root node
                turnerHtml = this.getTurnerHtml(CCTREE_OPEN_TOP_SRC,
                    CCTREE_TURNER_OPEN_ALTTXT);
            } else {
                turnerHtml = this.getTurnerHtml(CCTREE_OPEN_MIDDLE_SRC,
                    CCTREE_TURNER_CLOSED_ALTTXT);
            }

            this.render(lhsHtml + turnerHtml, depth);

            // add a tree hierarchy line to the left hand side html
            lhsHtml += CCTREE_LINE_IMG;

            // store the fact that we're not the last sibling
            this.isLastSibling = false;
        }
    } else {
        // we're at the root node level
        this.render("", depth);
    }
    
    // now display any child nodes
    if (this.numChildren > 0) {
        // increment the curent depth
        depth++;

        // loop thru all child nodes
        for (i = 0; i < this.numChildren; i++)  {
            this.children[i].tree = this.tree;
            if (i == this.numChildren - 1) {
                // this is the last of the children
                this.children[i].init(depth, true, lhsHtml);
            } else {
                // not the last child node
                this.children[i].init(depth, false, lhsHtml);
            }
        }
    }

    if (is_nav4up) {
        if (this.tree.selectedId == this.id) {
            this.layer.document.layers["sLabel"].visibility = "show";
            this.layer.document.layers["label"].visibility = "hidden";
        } else {
            this.layer.document.layers["label"].visibility = "show";
            this.layer.document.layers["sLabel"].visibility = "hidden";
        }
        this.layer.document.layers["pLabel"].visibility = "hidden";
        
        // set the turner image object
        this.turnerImgObj = this.layer.document.images["turnerIcon" + this.id];
    } else {
        // set the turner image object in other browsers
        this.turnerImgObj = doc.getElementById("turnerIcon" + this.id);
    }
}

// This OO function maps to CCContainerNode:setOpen(isOpen)
//
// It will open or close a container based on the given argument
CCContainerNode.prototype.setOpen = function(isOpen) {
    var i;
    var totalHeight;
    
    if (isOpen == this.isOpen) {
        // no change in open state
        return;
    }

    if (is_nav4up && this.isVisible()) {
        // handle netscape 4
        totalHeight = 0;
        
        // total up the height of all children
        for (i = 0; i < this.numChildren; i++) {
            var child = this.children[i];
            totalHeight += child.layer.clip.height;
            if (child.children != null) {
                totalHeight += child.getHeightOfChildren();
            }
        }

        if (this.isOpen) {
            // this node is closing
            totalHeight = 0 - totalHeight;
        }
        
        for (j = this.id + this.getNumDescendants() + 1; j < this.tree.nodes.length; j++) {
            // move the next visible node up or down
            this.tree.nodes[j].layer.moveBy(0, totalHeight);
        }
    }
    
    this.isOpen = isOpen;
    
    this.tree.updateDisplay(this);
}

// This OO function maps to CCContainerNode:render(lhsHtml, depth)
//
// This method outputs the html necessary to display this node. It prepends
// this output with the given left hand side html
CCContainerNode.prototype.render = function(lhsHtml, depth) {
    // define left hand pixel margin for ns4
    var left = (depth + 1) * 18 + CCTREE_SPACER_WIDTH + 3 - depth * 2 + 3;

    if (this.iconWidth < CCTREE_NODE_IMG_WIDTH) {
        // width of node image is less than default
        left -= CCTREE_NODE_IMG_WIDTH - this.iconWidth;
    } else if (this.iconWidth > CCTREE_NODE_IMG_WIDTH) {
        // node image is wider than default
        left += this.iconWidth - CCTREE_NODE_IMG_WIDTH;
    }
    
    if (this.isRoot) {
        doc.write(this.getTableStart(null, "30"));
    } else {
        doc.write(this.getTableStart(null, null));
    }

    if (lhsHtml != "") {
        // output the left hand side html we were given
        doc.write(lhsHtml);
    }

    // write out the td for the node image
    doc.write("</td>" + CCTREE_TD);

    var linkNode = this.url != "null";

    // test if this node is clickable
    if (linkNode) {
        // output the link for this nodes image
        doc.write(this.getOpenAnchor(false));
    }
    
    // output the image for this node
    doc.write(this.icon);
    
    if (linkNode) {
        // close the image link
        doc.write("</a>");
    }

    if (!is_nav4up) {
         // append a spacer
        doc.write("<img src=\"" + CCIMAGE_DOT_SRC + "\" width=\"3\" ");
        doc.write("border=\"0\" height=\"13\" align=\"bottom\"></img>");
    } else {
        // netscape 4 has limited dhtml suport so we need to jump thru some
        // hoops to support the desired UI effects. We'll output 3 layers, each
        // containing the nodes label but with a different appearance. Layer
        // label is the default non selected style, pLabel is the selection 
        // parent highlight (just bold) and sLabel is the selected node style
        // (bold and black)

        doc.write("<layer id=\"label\" visibility=\"show\" ");
        doc.write("left=\"" + left + "\" top=\"5\">");
    }

    if (linkNode) {
        var openAnchor = this.getOpenAnchor(true);
        
        // write out the label as a link
        doc.write(openAnchor);
    }
    
    doc.write(this.label);

    if (linkNode) {
        // close the anchor
        doc.write("</a>");
    }

    if (is_nav4up) {
        doc.write("</layer>");
        
        if (linkNode) {
            // append the hidden style layers
            doc.write("<layer id=\"pLabel\" visibility=\"show\" ");
            doc.write("left=\"" + left + "\" top=\"5\">");
            doc.write(openAnchor + "<strong>" + this.label + "</strong></a>");
            doc.write("</layer>");
            doc.write("<layer id=\"sLabel\" visibility=\"show\" ");
            doc.write("left=\"" + left + "\" top=\"5\">");
            doc.write(openAnchor + "<strong><font color=\"black\">" + this.label);
            doc.write("</font></strong></a></layer>");
        }
    }
    
    // close the label td, tr and table
    doc.write("</td>\n<td nowrap=\"nowrap\" width=\"99%\">&nbsp;</td></tr>");
    doc.write("</table>\n");
    
    if (is_nav4up) {
        // close the layer containing this node's table
        doc.write("</layer>");
        // set the dhtml element for this node to be the layer
        this.layer = doc.layers["node" + this.id];
        // adjust the document's vertical position
        doc.yPos = doc.yPos + this.layer.clip.height;
    } else {
        // set the dhtml element for this to be the table we created
        this.layer = doc.getElementById("node" + this.id);
    }
}

// This OO function maps to CCContainerNode::getNumDescendants()
//
// Returns the number of descendants this node has
CCContainerNode.prototype.getNumDescendants = function() {
    var numDescendants = this.numChildren;
    var i = 0;

    // loop thru each child
    for (i = 0; i < this.numChildren; i++){
        // test if child is a container node
        if (this.children[i].children) {
            // it is so add it's children to sub entries
            numDescendants += this.children[i].getNumDescendants();
        }
    }
    
    return numDescendants;
}

/*****************************************************************************/
/* Define an ojbect class that extends the abstract tree node class          */
/*****************************************************************************/
function CCObjectNode(label, statusTxt, url, icon, key, iconWidth, onClick, target) {
    this.base = CCAbstractTreeNode;
    this.base(label, statusTxt, url, icon, key, iconWidth, onClick, target);
}

// CCObjectNode extends CCAbstractTreeNode
CCObjectNode.prototype = new CCAbstractTreeNode;

// This OO function maps to CCObjectNode:display()
//
// This method will show the object
CCObjectNode.prototype.display = function() {
    this.doDisplay();
}

// This OO function maps to CCObjectNode:hide()
//
// This method will hide the object
CCObjectNode.prototype.hide = function() {
    this.doHide();
}

// This OO function maps to CCObjectNode:init()
//
// This method is called to initialize the node
CCObjectNode.prototype.init = function(depth, lastNode, lhsHtml) {
    
    this.createIndex();
    
    if (depth > 0) {
	if (this.isTopNode() && lastNode) {
	    // there is only one node in the tree
            this.render(lhsHtml, depth);
	} else if (this.isTopNode()) {
            // the top node is a leaf node
	    this.render(lhsHtml + CCTREE_LINE_FIRST_IMG, depth);
	} else if (lastNode)  {
            // the last sibling in the children array
            this.render(lhsHtml + CCTREE_LINE_LAST_IMG, depth);
        } else {
            // not the last sibling, use middle line
            this.render(lhsHtml + CCTREE_LINE_MIDDLE_IMG, depth);
        }
    } else {
        this.render("", depth);
    }
}

// This OO function maps to CCObjectNode::render(lhsHtml. depth)
//
// This method will output the html necessary to display this object node. It
// prepends the given left hand side html to this output
CCObjectNode.prototype.render = function(lhsHtml, depth) {
    // define left hand pixel margin for ns4
    var left = (depth + 1) * 18 + CCTREE_SPACER_WIDTH + 3 - depth * 2 + 3;

    if (this.iconWidth < CCTREE_NODE_IMG_WIDTH) {
        // width of node image is less than default
        left -= CCTREE_NODE_IMG_WIDTH - this.iconWidth;
    } else if (this.iconWidth > CCTREE_NODE_IMG_WIDTH) {
        // node image is wider than default
        left += this.iconWidth - CCTREE_NODE_IMG_WIDTH;
    }
    
    doc.write(this.getTableStart(null, null));

    doc.write(lhsHtml + "</td>" + CCTREE_TD);

    var linkNode = this.url != "null";
    
    // test if this node is clickable
    if (linkNode) {
        // write the anchor for it
        doc.write(this.getOpenAnchor(false));
    }

    doc.write(this.icon);

    if (linkNode) {
        doc.write("</a>");
    }

    if (!is_nav4up) {
         // append a spacer
        doc.write("<img src=\"" + CCIMAGE_DOT_SRC + "\" width=\"3\" ");
        doc.write("border=\"0\" height=\"13\" align=\"bottom\"></img>");
    } else {
        doc.write("<layer id=\"label\" visibility=\"show\" ");
        doc.write("left=\"" + left + "\" top=\"5\">");
    }

    if (linkNode) {
        var openAnchor = this.getOpenAnchor(true);
        doc.write(openAnchor);
    }

    doc.write(this.label);
    
    if (linkNode) {
        doc.write("</a>");
    }
    
    if (is_nav4up) {
        doc.write("</layer>");

        if (linkNode) {
            // node is linked, write hidden style layers
            doc.write("<layer id=\"pLabel\" visibility=\"show\" ");
            doc.write("left=\"" + left + "\" top=\"5\">");
            doc.write(openAnchor + "<strong>" + this.label + "</strong></a>");
            doc.write("</layer>");
            doc.write("<layer id=\"sLabel\" visibility=\"show\" ");
            doc.write("left=\"" + left + "\" top=\"5\">");
            doc.write(openAnchor + "<strong><font color=\"black\">" + this.label);
            doc.write("</font></strong></a></layer>");
        }
    }

    // close the label td, tr and table
    doc.write("</td><td nowrap=\"nowrap\" width=\"99%\">&nbsp;</td></tr>");
    doc.write("</table>\n");

    if (is_nav4up) {
        // close the layer containing this node's table
        doc.write("</layer>");
        this.layer = doc.layers["node" + this.id];
        doc.yPos = doc.yPos + this.layer.clip.height;
    } else if (is_ie4up) {
        this.layer = doc.getElementById("node" + this.id);
    }
}

/*****************************************************************************/
/* Define a tree object class                                                */
/*****************************************************************************/
function CCTree(name) {
    // the distinct name of this tree
    this.name = name;
    // an array containing the top or root level nodes
    this.rootLevelNodes = new Array;
    // an array containing all the nodes in this tree
    this.nodes = new Array;
    // the html layer of the selected item
    this.selectedLayer = null;
    // the id of the selected item
    this.selectedId = -1;
    // the selected node
    this.selectedNode = null;
    // whether or not this tree should save state via a cookie
    this.isPersistent = false;
    // the target to open links for this tree in 
    this.target = null;
}

// This OO function maps to CCTree::addChild(node)
//
// This method will called to add a top / root level node to a tree.
CCTree.prototype.addChild = function(node) {
    if (node == null) {
        return;
    }

    this.rootLevelNodes[this.rootLevelNodes.length] = node;
    this.nodes[this.nodes.length] = node;
    node.tree = this;
}

// This OO function maps to CCTree::createIndex(node)
//
// This method will called to create an index for the given node in this tree's
// array of nodes
CCTree.prototype.createIndex = function(node) {
    this.nodes[this.nodes.length] = node;
    node.tree = this;
    return this.nodes.length - 1;
}

// This OO function maps to CCTree::getNode(targetId)
//
// This method will return the node with the given id, or null if no such node
// was found in this tree
CCTree.prototype.getNode = function(targetId) {
    if (this.nodes == null) {
    // no nodes, return null
        return null;
    }

    var targetNode = null;

    // loop through all the nodes in this tree checking id's
    for (var i = 0; i < this.nodes.length; i++) {
        if (this.nodes[i].key == targetId) {
            return this.nodes[i];
        }
    }

    return targetNode;
}

// This OO function maps to CCTree::init(openContainers)
//
// This method will init the tree at load time
CCTree.prototype.init = function(openContainers) {
    var i;
    var cookie = document.cookie;
    
    if (is_nav6up) {
        // Use ie4 implementation for Netscape 6
        is_nav4up = 0;
        is_ie4up = 1;
    }
    
    if (this.rootLevelNodes.length > 1 || !this.rootLevelNodes[0].isRoot) {
        var fakeRoot = new CCContainerNode("", "", "", "", "", "");

        for (i = 0; i < this.rootLevelNodes.length; i++) {
            var n = this.rootLevelNodes[i];
            fakeRoot.addChild(n);
            if (i + 1 < this.rootLevelNodes.length) {
                n.init(1, false, "");
            } else {
                n.init(1, true, "");
            }
            n.display();
        }

    } else {
        this.rootLevelNodes[0].init(0, true, "");
        // display the node
        this.rootLevelNodes[0].display();
    }
    
    if (is_nav4up) {
        doc.write("<layer top=");
        doc.write(this.nodes[this.nodes.length - 1].layer.top);
        doc.writeln(">&nbsp;</layer>");
    }
    
    // close the whole tree
    for (i = 0; i < this.rootLevelNodes.length; i++) {
        // collapse any / all the containers in the tree
        if (this.rootLevelNodes[i].isOpen != null) {
            // is a container node
            this.rootLevelNodes[i].collapseAll()
            if (this.rootLevelNodes[i].isRoot) {
                // expand any root nodes
                this.rootLevelNodes[i].setOpen(true);
            }
        }
    }

    if (is_nav4up) {
        // loop thru all seleced label layers and hide them
        for (i = 0; i < document.layers.length; i++) {
            var sLayer = document.layers[i].document.layers["sLabel"];
            if (sLayer != null) {
                sLayer.visibility = "hidden";
                var pLayer = document.layers[i].document.layers["pLabel"];
                pLayer.visiblity = " hidden";
            }
        }
    }

    for (var i = 0; i < openContainers.length; i++) {
        this.openOrClose(this.getNode(openContainers[i]).id);
    }

    if (this.isPersistent) {
        // restore the selected item and open containers
        this.restoreState(cookie);
        this.saveState();
    }
}

// This OO function maps to CCTree::openOrClose(id)
//
// This method will called to open or close the container with the given id
CCTree.prototype.openOrClose = function(id) {
    var container;
    var state;
    
    container = this.nodes[id];
    state = container.isOpen;
    
    container.setOpen(!state);
    
    if (this.isPersistent) {
        // save the state via cookie
        this.saveState();
    }
}

// This OO function maps to CCTree::updateDisplay(container)
//
// This method will called when a container node in this tree is opened or
// closed. It will perform the necessary DHTML to render the new tree state.
CCTree.prototype.updateDisplay = function(container) {
    var i = 0;
    var child;
    var layer;
    var p;
    
    if (container.isOpen) {
        // container opened
        if (container.turnerImgObj) {
            if (container.isLastSibling) {
                if (container.isTopNode()) {
                    // top node with no more siblings
                    container.turnerImgObj.src = CCTREE_OPEN_TOP_NS_SRC;
                } else {
                    // last sibling, use handle open last image
                    container.turnerImgObj.src = CCTREE_OPEN_LAST_SRC;
                }
            } else {
                if (container.isTopNode()) {
                    // top node but not last sibling
                    container.turnerImgObj.src = CCTREE_OPEN_TOP_SRC;
                } else {
                    // not last sibling, use handle open middle
                    container.turnerImgObj.src = CCTREE_OPEN_MIDDLE_SRC;
                }
            }
            container.turnerImgObj.alt = CCTREE_TURNER_OPEN_ALTTXT;
            container.turnerImgObj.title = CCTREE_TURNER_OPEN_ALTTXT;
        }

        var selectionNowVisible = false;

        // loop thru and display all children
        for (i = 0; i < container.numChildren; i++) {
			var child = container.children[i];
			child.display();
			if (child.id == this.selectedId || 
					container.hasDescendant(this.selectedId)) {
				selectionNowVisible = true;
			}
        }

		// the selected node may now be visible
		if (selectionNowVisible) {
			// selected node is now visible
			// remove the selection parent highlight
            if (is_nav4up) {
                layer =
                    doc.layers["node" + container.id].document.layers["pLabel"];
                layer.visibility = "hidden";
                layer =
                    doc.layers["node" + container.id].document.layers["label"];
                layer.visibility = "show";
            } else {
                doc.getElementById("node" + container.id).style.fontWeight =
                    "";
            }
		}
    } else {
        // container closed
        if (container.turnerImgObj) {
            if (container.isLastSibling) {
                if (container.isTopNode()) {
                    // top node with no siblings
                    container.turnerImgObj.src = CCTREE_CLOSED_TOP_NS_SRC;
                } else {
                    // last sibling
                    container.turnerImgObj.src = CCTREE_CLOSED_LAST_SRC;
                }
            } else {
                if (container.isTopNode()) {
                    // top node with more siblings
                    container.turnerImgObj.src = CCTREE_CLOSED_TOP_SRC;
                } else {
                    // last sibling
                    container.turnerImgObj.src = CCTREE_CLOSED_MIDDLE_SRC;
                }
            }
            container.turnerImgObj.alt = CCTREE_TURNER_CLOSED_ALTTXT;
            container.turnerImgObj.title = CCTREE_TURNER_CLOSED_ALTTXT;
        }

        // loop thru and hide all the children of this node
        for (i = 0; i < container.children.length; i++) {
            container.children[i].hide();
        }

        // the selected node may now be hidden
        if (this.selectedId != -1 && this.selectedId != container.id &&
                container.hasDescendant(this.selectedId) && !container.isRoot) {
            // either this node is selected or the selected node is now hidden
            // so give this node the selection parent highlight
            if (is_nav4up) {
                layer =
                    doc.layers["node" + container.id].document.layers["label"];
                layer.visibility = "hidden";
                layer =
                    doc.layers["node" + container.id].document.layers["pLabel"];
                layer.visibility = "show";
            } else {
                doc.getElementById("node" + container.id).style.fontWeight =
                    "bold";
            }
        }
    }

    if (this.isPersistent == true) {
        this.saveState();
    }
}

// This OO function maps to CCTree::doSelection(id)
//
// This method will perform the necessary DHTML to show the node with the given
// id as being selcted
CCTree.prototype.doSelection = function(id) {
    var parentNode = null;
    var selectedLabel = null;
    var layer = null;

    if (this.selectedLayer != null) {
        // remove prior selection
        if (is_nav4up) {
            this.selectedLayer.bgColor = CCTREE_BGCOLOR;
            selectedLabel = this.selectedLayer.document.layers["sLabel"];
            selectedLabel.visibility = "hidden";
            if (this.selectedNode.isVisible()) {
                selectedLabel = this.selectedLayer.document.layers["label"];
                selectedLabel.visibility = "show";
            }
            // also remove prior selection parent highlight
            parentNode = this.selectedNode.parent;
            while (parentNode != null && parentNode.layer != null) {
                var pId = parentNode.id;
                if (parentNode.isVisible()) {
                    layer = doc.layers["node" + pId].document.layers["label"];
                    layer.visibility = "show";
                }
                layer = doc.layers["node" + pId].document.layers["pLabel"];
                if (layer != null) {
                    layer.visibility = "hidden";
                }
                parentNode = parentNode.parent;
            }
        } else {
            if (!this.selectedNode.isRoot) {
                // remove selected row stle from non-root nodes
                doc.getElementById("row" + this.selectedId).className = "";
            }
            this.selectedLayer.style.fontWeight = "";
            selectedLabel = doc.getElementById("labelfor" + this.selectedId);
            selectedLabel.style.color = "";
            parentNode = this.selectedNode.parent;
            while (parentNode != null) {
                layer = doc.getElementById("node" + parentNode.id);
                if (layer != null) {
                    layer.style.fontWeight = "";
                }
                parentNode = parentNode.parent;
            }
        }
    }

    // update the current selection id and get the selected node object
    this.selectedId = id;
    this.selectedNode = this.nodes[id];

    if (is_nav4up) {
        this.selectedLayer = doc.layers["node" + id];
        if (!this.selectedNode.isRoot) {
            // only update row color for non-root nodes
            this.selectedLayer.bgColor = CCTREE_SELECTEDCOLOR;
        }
        selectedLabel = this.selectedLayer.document.layers["sLabel"];
        selectedLabel.visibility = "show";
        selectedLabel = this.selectedLayer.document.layers["label"];
        selectedLabel.visibility = "hidden";
        selectedLabel = this.selectedLayer.document.layers["pLabel"];
        selectedLabel.visibility = "hidden";
        parentNode = this.selectedNode.parent;
        if (parentNode != null && !parentNode.isRoot && !parentNode.isOpen) {
            var pId = parentNode.id;
            var pLayer = doc.layers["node" + pId];
            if (pLayer != null) {
                layer = pLayer.document.layers["label"];
                if (layer != null) {
                    layer.visibility = "hidden";
                }
                layer = pLayer.document.layers["pLabel"];
                if (layer != null) {
                    layer.visibility = "show";
                }
            }
        }
    } else if (is_ie4up) {
        this.selectedLayer = doc.getElementById("node" + id);
        if (!this.selectedNode.isRoot) {
            // only update row color for non-root nodes
            doc.getElementById("row" + id).className = CCTREE_SELECTED_ROW;
        }
        this.selectedLayer.style.fontWeight = "bold";
        var selectedLabel = doc.getElementById("labelfor" + id);
        selectedLabel.style.color = "black";
        parentNode = this.selectedNode.parent;
        if (parentNode != null && !parentNode.isRoot && !parentNode.isOpen) {
            layer = doc.getElementById("node" + parentNode.id);
            if (layer != null) {
                layer.style.fontWeight = "bold";
            }
        }
    }

    if (this.isPersistent) {
        // save the state via cookie
        this.saveState();
    }
}

// This OO function maps to CCTree::yokeTo(key)
//
// This method will set the node with the given key to be the selected node. It
// will also ensure that the node is visible. It will however NOT open the url
// associated with the node (use yokeToAndLoad(key) for that)
CCTree.prototype.yokeTo = function(key) {
    var i;
    var found = false;

    for (i = 0; i < this.rootLevelNodes.length && !found; i++) {
        found = this.doYoke(this.rootLevelNodes[i], key, false);
    }

    if (found && is_ie4up) {
        var id = this.getNode(key).id;
        
        window.scrollTo(0, document.getElementById("node" + id).offsetTop);
    }
}

// This OO function maps to CCTree::yokeToAndLoad(key)
//
// This method will set the node with the given key to be the selected node. It
// will also ensure that the node is visible and open the url associated with
// this node in the target frame
CCTree.prototype.yokeToAndLoad = function(key) {
    var i;
    var found = false;

    for (i = 0; i < this.rootLevelNodes.length && !found; i++) {
        found = this.doYoke(this.rootLevelNodes[i], key, true);
    }

    if (found && is_ie4up) {
        var id = this.getNode(key).id;
        
        window.scrollTo(0, document.getElementById("node" + id).offsetTop);
    }
}

// This OO function maps to CCTree::doYoke(node, key, loadUrl)
//
// This method will set the node with the given key to be the selected node. It
// will also ensure that the node is visible. Depending on the value specified
// for loadUrl, it will load (or not) the url associated with the yoke to node
// in the target frame
CCTree.prototype.doYoke = function(node, key, loadUrl) {
    var i;
    var found = false;

    if (node.key == key) {
        this.doSelection(node.id);
        // open all parent containers to ensure yoke to node is visible
        var p = node.parent;
        while (p != null) {
            p.setOpen(true);
            p = p.parent;
        }

        // see if the we should open the url associated with this node
        if (loadUrl) {
            window.open(node.url, this.target);
        }

        // the given node is the yoke to node
        return true;
    }

    // this node is not the yoke to node, check any children
    if (node.children != null) {
        for (i=0; i< node.children.length && !found; i++) {
            found = this.doYoke(node.children[i], key, loadUrl);
        }
    }

    // return whether the yoke succeeded
    return found;
}

// This OO function maps to CCTree::saveState()
//
// This method will save the state of the tree in a client side browser cookie
CCTree.prototype.saveState = function() {
    var i;
    // create a cookie containing the id of the selection and open containers
    var cookie = this.selectedId + "&";

    for (i = 0; i < this.rootLevelNodes.length; i++) {
        cookie += this.saveNodeState(this.rootLevelNodes[i]);
    }

    // store the cookie
    document.cookie = "cctree_" + this.name + "=" + cookie;
}

// This OO function maps to CCTree::saveNodeState(node)
//
// This method is used by saveState to store the state of the tree
CCTree.prototype.saveNodeState = function(node) {
    var i;
    var cookie = "";

    if (node.children != null) {
        // container node
        if (node.isOpen) {
            // its open, save its key in the cookie
            cookie += node.key + ".";
        }
        
        for (i = 0; i < node.children.length; i++) {
            // also save the state of any child containers
            cookie += this.saveNodeState(node.children[i]);
        }
    }

    return cookie;
}

// This OO function maps to CCTree::restoreState(cookie)
//
// This method will attempt to restore the tree state from the given cookie
CCTree.prototype.restoreState = function(cookie) {
    if (cookie != null) {
        var prefix = "cctree_" + this.name + "=";
        
        pos = cookie.indexOf(prefix);

        if (pos >= 0) {
            // there is a state cookie, restore it
            cookie = cookie.substring(pos + prefix.length, cookie.length);
            cookie = cookie.substring(0, cookie.indexOf(";"));

            // determine the selected node id
            sId = cookie.substring(0, cookie.indexOf("&"));
            sId = parseInt(sId);
            if (!isNaN(sId) && sId != -1) {
                // if we got one, yoke to the node with sId
                this.yokeTo(this.nodes[sId].key);
            }

            // the rest of the cookie contains a list of open container ids
            cookie = cookie.substring(cookie.indexOf("&") + 1);
            var cookie_keys = cookie.split(".");
            var keys = new Array;
            
            for (var i = 0; i < cookie_keys.length - 1; i++) {
                var key = cookie_keys[i];
                keys[key] = true;
            }

            // loop thru and restore the state of all container nodes
            for (var i = 0; i < this.rootLevelNodes.length; i++) {
                // before restoring open containers, close all of them since
                // some initially open nodes may not be after page reload
                if (this.rootLevelNodes[i].children) {
                    // has a children array, must be a container so close it
                    this.rootLevelNodes[i].collapseAll();
                }
                // recursively restore the state of this node & its descendants
                this.restoreNodeState(this.rootLevelNodes[i], keys);
            }
        }
    }
}

// This OO function maps to CCTree::restoreNodeState(node, keys)
//
// This method will restore the state of the given node according to the given
// list of open container ids (the keys param)
CCTree.prototype.restoreNodeState = function(node, keys) {
    var cookie = "";
    if (node.children) {
        var key = node.key;
        
        if (keys[key]) {
            // this node is open
            node.setOpen(true);
        }
        
        var i;
        
        for (i = 0; i < node.children.length; i++) {
            cookie = this.restoreNodeState(node.children[i], keys);
        }
    }

    return cookie;
}

// Global convience variables
var doc = document;
var CCTREE_TD = "<td nowrap=\"nowrap\">";
