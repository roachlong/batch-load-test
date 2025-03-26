create table Batch (connections integer, duration integer, size integer, throughput float(53), elapsed bigint, records bigint, startTime timestamp(6) with time zone, statements bigint, transactions bigint, id uuid not null, status varchar(255), type varchar(255), primary key (id));
create table Child (childId integer not null, parentKey varchar(255) not null, primary key (childId, parentKey));
create table Parent (nextCycle integer not null, numericId bigint not null, characterId varchar(255), parentKey varchar(255) not null, type varchar(255) not null, primary key (parentKey));
create table Record (leader boolean, createdOn timestamp(6) with time zone, updatedOn timestamp(6) with time zone, process_id uuid not null, processName varchar(255) not null, primary key (process_id));
create table Run (connections integer, duration integer, size integer, throughput float(53), elapsed bigint, modifications bigint, startTime timestamp(6) with time zone, statements bigint, transactions bigint, id uuid not null, status varchar(255), type varchar(255), primary key (id));
create index idx_parent_child on Child (parentkey, childid);
