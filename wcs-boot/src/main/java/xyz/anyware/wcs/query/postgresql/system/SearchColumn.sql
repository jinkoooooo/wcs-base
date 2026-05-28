select
	b.name,
	b.term ,
	b.ref_type,
	b.ref_name ,
	b.ref_name,
	b.search_editor,
	coalesce(b.search_oper, 'eq') as search_oper,
	b.search_rank,
	a.resource_url
from
	vue_menus a,
	menu_columns b
where
	a.name =:router
	and a.id = b.menu_id
	and (b.search_editor is not null
		or b.search_editor != 'hidden')
order by
	b.search_rank