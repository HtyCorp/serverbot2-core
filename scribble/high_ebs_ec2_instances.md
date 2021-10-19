### List of instances supporting >=1000MiB/s EBS bandwidth for volume prewarming ###

| family | min price (hourly)^ | min size required | APS2 available | notes |  
|------|------|----------|---|-|  
| c6gn | .04  | c6gn.m   | n | very cheap; uses burst credits |  
| m6i  | .10  | m6i.l    | n | very cheap; uses burst credits |  
| r5b  | .15  | r5b.l    | n | same story as c6gn/m6i, baseline EBS is higher but burst is similar |  
| c6g  | 1.1  | c6g.8xl  | y | |  
| m6g  | 1.2  | m6g.8xl  | y | |  
| c5   | 1.5  | c5.9xl   | y | |  
| r6g  | 1.6  | r6g.8xl  | y | |  
| m5zn | 2.0  | m5zn.6xl | y | slightly lower than usual for m/c |  
| g4dn | 2.1  | g4dn.8xl | y | |  
| z1   | 2.2  | z1d.6xl  | y | |  
| m5   | 2.3  | m5.12xl  | y | |  
| m5a  | 2.7  | m5a.16xl | y | |  
| x2   | 2.7  | x2gd.8xl | n | surprisingly better than x1/e |  
| r5   | 3.0  | r5.12xl  | y | same as m family |  
| m4   | 3.2  | m4.16xl  | y | |  
| c5a  | 3.7  | c5a.24xl | y | |  
| x1   | 13.3 | x1.32xl  | y | just not right |  
| p2   | 14.4 | p2.16xl  | y | very costly due to GPU |
| x1e  | 26.7 | x1e.32xl | y | just not right |
