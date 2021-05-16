#!/usr/bin/env python
# coding: utf-8

# In[1]:


import pandas as pd
import numpy as np
from pathlib import Path
import matplotlib.pyplot as plt
import networkx as nx
import matplotlib
matplotlib.use('Qt5Agg')

def raise_window(figname=None):
    if figname: plt.figure(figname)
    cfm = plt.get_current_fig_manager()
    cfm.window.activateWindow()
    cfm.window.raise_()

# load interactions
datafolder = r"C:\Users\chadw\eclipse-workspace\EM426-ABM"
aname = "agent_report.txt"
ename = "interaction_report.txt"

G = nx.Graph()
nodes = pd.read_csv(Path(datafolder,aname),header=None,names=['node'])
data = nodes.set_index('node').to_dict('index').items()
G.add_nodes_from(data)

edges = pd.read_csv(Path(datafolder,ename),header=None,names=['node1','node2','weight'])
G = nx.from_pandas_edgelist(edges, 'node1', 'node2', edge_attr=True)
nodes = pd.read_csv(Path(datafolder,aname),header=None,names=['node'])
data = nodes.set_index('node').to_dict('index').items()
G.add_nodes_from(data)

edges,weights = zip(*nx.get_edge_attributes(G,'weight').items())

plt.figure('interaxplot',figsize=(20,10))
nx.draw_circular(G,#pos,
                node_color='r', 
                node_size=200,
                font_size=24,
                edgelist=edges, 
                edge_color=weights, 
                width=10.0, 
                edge_cmap=plt.cm.Blues,
                with_labels=True, 
                font_weight='bold', 
                verticalalignment='bottom')

plt.savefig('edges.png')

#raise_window('interaxplot')