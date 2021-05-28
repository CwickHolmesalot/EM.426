#!/usr/bin/env python
# coding: utf-8

# In[29]:


import pandas as pd
import numpy as np
from pathlib import Path
import matplotlib.pyplot as plt
import networkx as nx

nagents = 5
ncycles = 200
ndemand0 = 20
probnew = 0

# load interactions
datafolder = r"C:\Users\chadw\eclipse-workspace\EM426-ABM"
#aname = "agent_report_{}_{}_{}_{}.txt".format(nagents,ncycles,ndemand0,probnew)
#ename = "interaction_report_{}_{}_{}_{}.txt".format(nagents,ncycles,ndemand0,probnew)
aname = "agent_report.txt"
ename = "interaction_report.txt"

#%matplotlib inline


# In[30]:


G = nx.Graph()
nodes = pd.read_csv(Path(datafolder,aname),header=None,names=['node'])
data = nodes.set_index('node').to_dict('index').items()
G.add_nodes_from(data)
#nx.draw(G,with_labels=True);

edges = pd.read_csv(Path(datafolder,ename),header=None,names=['node1','node2','weight'])
G = nx.from_pandas_edgelist(edges, 'node1', 'node2', edge_attr=True)
nodes = pd.read_csv(Path(datafolder,aname),header=None,names=['node'])
data = nodes.set_index('node').to_dict('index').items()
G.add_nodes_from(data)

edges,weights = zip(*nx.get_edge_attributes(G,'weight').items())

plt.figure(figsize=(20,20))
h=nx.draw_circular(G,#pos,
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

