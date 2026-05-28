select  t.name as parent_name,
		t.icon_path as parent_icon,
		t.id as p_id,
		t.parent_id,
		t.rank as parent_rank,
		a.id,
		a.routing,
		a.template ,
		a.name ,
		a.icon_path,
		a.rank,
		case
			when hidden_flag = false  then 0
			else  1  end hidden_flag
from
(
		select id, name, template, id as parent_id,  rank, routing, icon_path
		from vue_menus
		where domain_id = :domainId
		  and category ='STANDARD'
		  and (parent_id is null or parent_id='')
) t left join vue_menus a on t.id = a.parent_id
order by t.rank, a.rank