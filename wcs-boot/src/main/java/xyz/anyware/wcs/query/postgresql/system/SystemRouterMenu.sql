select  t.name as parent_name,
		t.parent_id,
		t.routing,
		coalesce(a.name,t.name) as name,
		a.routing as path,
		a.template as component,
		a.name as  redirect,
		a.name as  title,
		coalesce(a.icon_path, '') as icon,
		false as hideChildrenInMenu,
		false as hideBreadcrumb,
		coalesce(a.hidden_flag,false) as hideMenu,
		a.routing as  currentActiveMenu
from
(
		select id, name, template, id as parent_id, '1' as menu_level, rank, routing
		from menus
		where domain_id = :domainId
		  and (parent_id is null or parent_id='')
) t left join menus a on t.id = a.parent_id
order by t.rank, a.rank