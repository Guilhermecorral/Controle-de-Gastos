alter table investment_movements add column tax_status_override varchar(30);
alter table investment_movements add column tax_withheld_override numeric(19,2);
alter table investment_movements add column tax_note varchar(255);
